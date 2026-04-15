<div align="center">

# 🛩️ Painel SISPAGAER — Backend

**API de dados de folha de pagamento do Comando da Aeronáutica**

![Java](https://img.shields.io/badge/Java-25_LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Quarkus](https://img.shields.io/badge/Quarkus-3.34.3-4695EB?style=for-the-badge&logo=quarkus&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)
![License](https://img.shields.io/badge/Uso_Restrito-FAB-009B3A?style=for-the-badge)

</div>

---

## 📋 Índice

- [Sobre o Projeto](#-sobre-o-projeto)
- [Funcionalidades](#-funcionalidades)
- [Arquitetura](#-arquitetura)
- [Stack Tecnológica](#-stack-tecnológica)
- [Modelo do Data Warehouse](#-modelo-do-data-warehouse)
- [Endpoints da API](#-endpoints-da-api)
- [Segurança](#-segurança)
- [Cache](#-cache)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Pré-requisitos](#-pré-requisitos)
- [Configuração e Execução](#-configuração-e-execução)
- [Variáveis de Ambiente](#-variáveis-de-ambiente)
- [Deploy em Kubernetes](#-deploy-em-kubernetes)
- [Testes](#-testes)
- [Desenvolvedor](#-desenvolvedor)
- [Ideias para o Futuro](#-ideias-para-o-futuro)

---

## 📌 Sobre o Projeto

O **Painel SISPAGAER Backend** é a camada de API REST que alimenta o painel analítico de folha de pagamento do **Comando da Aeronáutica (COMAER)**. A aplicação realiza consultas analíticas a um **Data Warehouse (DW)** já existente e expõe os dados em forma de endpoints JSON consumidos pelo frontend do painel.

### Contexto

A gestão da folha de pagamento de uma força armada envolve dezenas de milhares de militares, centenas de organizações militares distribuídas por todo o território nacional, e uma complexidade de rubricas, categorias e vínculos funcionais que exige ferramentas analíticas robustas. O painel SISPAGAER nasce da necessidade de transformar esse volume de dados em informação estratégica, acessível e visualmente clara para os gestores da DIRAD.

### O que este backend **não é**

> O pipeline de dados (ETL/ELT) que alimenta o DW é um sistema separado e está **fora do escopo** desta aplicação. Este backend é exclusivamente um **leitor** do DW — nunca escreve nem altera dados.

---

## ✨ Funcionalidades

### Endpoints de negócio

| Funcionalidade | Descrição |
|---|---|
| **Dashboard consolidado** | Retorna todos os KPIs do painel em uma única requisição, com 3 queries executadas em paralelo |
| **Totais por Posto/Graduação** | Agrega valores bruto, líquido e descontos por posto (CEL, TC, MAJ, CB, SD...) com filtros opcionais por OM e categoria |
| **Ranking de Organizações Militares** | Classifica as OMs pelo maior valor de folha no período, com limite configurável |
| **Série Histórica** | Retorna a evolução mensal da folha para os últimos N meses (até 120 meses / 10 anos) |
| **Dimensões do DW** | Lista postos/graduações e OMs para popular filtros e dropdowns no frontend |

### Características técnicas

| Característica | Detalhe |
|---|---|
| **Reatividade total** | Nenhuma thread da JVM é bloqueada — pool de conexões assíncrono com Vert.x |
| **Queries paralelas** | O dashboard dispara 3 queries simultâneas com `Uni.combine().all()` |
| **Cache multicamada** | Caffeine em memória com TTLs diferenciados por tipo de dado |
| **Fault Tolerance** | Circuit Breaker, Retry e Timeout em todos os endpoints críticos |
| **Documentação automática** | Swagger UI gerado em tempo de execução via SmallRye OpenAPI |
| **Observabilidade** | Health checks (readiness/liveness) para Kubernetes + métricas Prometheus |
| **Segurança** | Autenticação por API Key com comparação em tempo constante (anti timing-attack) + headers de segurança HTTP em todas as respostas |

---

## 🏛️ Arquitetura

```
┌─────────────────────────────────────────────────────────────────┐
│                        KUBERNETES CLUSTER                       │
│                                                                 │
│  ┌──────────┐   HTTPS/TLS   ┌─────────────────────────────┐    │
│  │          │◄──────────────►│     INGRESS CONTROLLER      │    │
│  │ Frontend │               └──────────────┬──────────────┘    │
│  │ (Painel) │                              │ HTTP interno       │
│  └──────────┘               ┌──────────────▼──────────────┐    │
│                             │   painel-sispagaer-backend   │    │
│                             │                             │    │
│                             │  ┌─────────────────────┐   │    │
│                             │  │     Resource Layer   │   │    │
│                             │  │  (JAX-RS / REST)     │   │    │
│                             │  └──────────┬──────────┘   │    │
│                             │             │               │    │
│                             │  ┌──────────▼──────────┐   │    │
│                             │  │    Service Layer     │   │    │
│                             │  │  Cache + FaultTol.   │   │    │
│                             │  └──────────┬──────────┘   │    │
│                             │             │               │    │
│                             │  ┌──────────▼──────────┐   │    │
│                             │  │  Repository Layer    │   │    │
│                             │  │  SQL explícito       │   │    │
│                             │  └──────────┬──────────┘   │    │
│                             └─────────────┼───────────────┘    │
│                                           │ MySQL Reativo       │
│                             ┌─────────────▼───────────────┐    │
│                             │      DATA WAREHOUSE          │    │
│                             │   (MySQL 8.4 — somente       │    │
│                             │    leitura por este backend) │    │
│                             └─────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### Decisões de design

- **Sem ORM/JPA:** o DW possui dimensões e fatos, não entidades. SQL explícito garante controle total sobre os planos de execução e evita o overhead de mapeamento objeto-relacional.
- **Cliente reativo (Vert.x):** o `MySQLPool` gerencia conexões assíncronas — sem bloqueio de threads, suportando alta concorrência com poucos recursos.
- **Prepared statements:** todos os parâmetros são posicionais (`?`), eliminando SQL Injection por design.
- **Records Java 25:** DTOs imutáveis com factory method `fromRow()` — sem getters/setters, sem boilerplate.

---

## 🛠️ Stack Tecnológica

| Camada | Tecnologia | Versão | Papel |
|---|---|---|---|
| Linguagem | Java LTS | 25 | Base da aplicação |
| Framework | Quarkus | 3.34.3 | Cloud-native, startup em ms |
| REST | Quarkus REST + Jackson | — | Endpoints JAX-RS reativos |
| Banco de Dados | MySQL | 8.4 | Data Warehouse |
| Driver DB | Vert.x Reactive MySQL Client | — | Pool reativo, async nativo |
| Cache | Caffeine (via Quarkus Cache) | — | Cache em memória com TTL |
| Documentação | SmallRye OpenAPI | — | Swagger UI automático |
| Resiliência | SmallRye Fault Tolerance | — | Circuit Breaker, Retry, Timeout |
| Observabilidade | SmallRye Health + Micrometer | — | Health checks + Prometheus |
| Contêiner | Docker | — | Empacotamento |
| Orquestração | Kubernetes | — | Deploy na FAB |
| Build | Maven Wrapper | 3.9.9 | Sem instalação global necessária |

---

## 🗄️ Modelo do Data Warehouse

O backend consulta um DW com schema estrela (star schema):

```
                    ┌───────────────────┐
                    │    dim_tempo      │
                    │───────────────────│
                    │ id_tempo (PK)     │
                    │ dat_competencia   │
                    │ ano               │
                    │ mes               │
                    │ trimestre         │
                    └────────┬──────────┘
                             │
  ┌──────────────────┐       │       ┌─────────────────────────┐
  │dim_posto_graduacao│       │       │  dim_organizacao_militar │
  │──────────────────│       │       │─────────────────────────│
  │ id_posto_grad(PK)│       │       │ id_om (PK)              │
  │ cod_posto_grad   │       │       │ cod_uasg                │
  │ sig_posto_grad   ├───────┤       │ sig_om                  │
  │ nom_posto_grad   │       │       │ nom_om                  │
  │ categoria        │  ┌────▼────┐  │ cod_comaer              │
  │ ord_hierarquia   │  │  FATO   │  │ nom_comaer              │
  └──────────────────┘  │─────────│  │ uf                      │
                        │fat_folha├──┤ municipio               │
  ┌──────────────────┐  │_pagament│  └─────────────────────────┘
  │   dim_rubrica    │  │o        │
  │──────────────────│  │─────────│  ┌─────────────────────────┐
  │ id_rubrica (PK)  ├──┤id_fat   │  │  dim_situacao_funcional  │
  │ cod_rubrica      │  │id_tempo │  │─────────────────────────│
  │ nom_rubrica      │  │id_posto │  │ id_situacao (PK)        │
  │ tipo             │  │id_om    ├──┤ cod_situacao            │
  │ categoria_rubrica│  │id_rubric│  │ nom_situacao            │
  └──────────────────┘  │id_situa │  └─────────────────────────┘
                        │cod_milit│
                        │val_bruto│
                        │val_liqui│
                        │val_desc │
                        └─────────┘
```

---

## 🔌 Endpoints da API

Base URL: `http://localhost:8080/api/v1`

> Todos os endpoints exigem o header `X-API-Key`. Consulte a documentação interativa em `/swagger-ui`.

### Folha de Pagamento

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/folha-pagamento/dashboard` | KPIs consolidados — 3 queries em paralelo |
| `GET` | `/folha-pagamento/totais-por-patente` | Totais agrupados por posto/graduação |
| `GET` | `/folha-pagamento/totais-por-om` | Ranking de OMs por valor de folha |
| `GET` | `/folha-pagamento/serie-historica` | Evolução mensal (até 120 meses) |

#### Parâmetros comuns

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `ano` | `int` | ✅ | Ano de competência (ex: `2024`) |
| `mes` | `int` | ✅ | Mês de competência, 1–12 (ex: `11`) |
| `codOm` | `string` | ❌ | Código UASG da OM (ex: `7236`) |
| `categoria` | `string` | ❌ | `OFICIAL`, `PRACA` ou `CIVIL` |
| `limite` | `int` | ❌ | Máx. de OMs retornadas (padrão: `15`) |
| `meses` | `int` | ❌ | Meses retroativos na série (padrão: `24`) |

### Dimensões

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/dimensoes/postos-graduacoes` | Lista postos e graduações (filtro por categoria) |
| `GET` | `/dimensoes/organizacoes-militares` | Lista OMs (filtro por COMAER) |

### Exemplos com `curl`

```bash
# Dashboard — competência Nov/2024
curl -H "X-API-Key: sua-chave" \
  "http://localhost:8080/api/v1/folha-pagamento/dashboard?ano=2024&mes=11"

# Totais por posto — somente Oficiais
curl -H "X-API-Key: sua-chave" \
  "http://localhost:8080/api/v1/folha-pagamento/totais-por-patente?ano=2024&mes=11&categoria=OFICIAL"

# Top 10 OMs por valor de folha
curl -H "X-API-Key: sua-chave" \
  "http://localhost:8080/api/v1/folha-pagamento/totais-por-om?ano=2024&mes=11&limite=10"

# Série histórica — últimos 24 meses
curl -H "X-API-Key: sua-chave" \
  "http://localhost:8080/api/v1/folha-pagamento/serie-historica?ano=2024&mes=11&meses=24"

# OMs do COMGAP
curl -H "X-API-Key: sua-chave" \
  "http://localhost:8080/api/v1/dimensoes/organizacoes-militares?codComaer=COMGAP"
```

---

## 🔒 Segurança

A aplicação não possui autenticação de usuário final (sem login). A comunicação entre o frontend e o backend é protegida por múltiplas camadas:

| Camada | Mecanismo | Onde configurar |
|---|---|---|
| **Autenticação** | API Key no header `X-API-Key` | Secret do Kubernetes → `API_KEY` |
| **Transporte** | TLS/HTTPS | Ingress Controller do cluster |
| **CORS** | Whitelist de origens | `CORS_ALLOWED_ORIGINS` |
| **Headers HTTP** | `X-Frame-Options`, `HSTS`, `Cache-Control: no-store`, etc. | Automático via `SecurityHeadersFilter` |
| **SQL Injection** | Prepared statements com `?` | Arquitetura (sem concatenação de SQL) |
| **Timing Attack** | Comparação de API Key em tempo constante | `ApiKeyFilter.constantTimeEquals()` |
| **Dados sensíveis** | `Cache-Control: no-store` em todas as respostas | Automático — dados de folha não são cacheados pelo browser |

> ⚠️ **Princípio fundamental:** nenhuma credencial, chave ou senha deve ser armazenada em arquivos do repositório. Toda informação sensível deve ser injetada via variáveis de ambiente ou Secrets do Kubernetes.

---

## ⚡ Cache

O cache em memória (Caffeine) reduz a carga sobre o DW para consultas repetidas. TTLs configurados por tipo de dado:

| Cache | TTL | Tamanho máximo | Justificativa |
|---|---|---|---|
| `dashboard` | 15 minutos | 50 entradas | Consulta pesada; resultado agregado de 3 queries |
| `totais` | 30 minutos | 200 entradas | Dados do período corrente |
| `serie-historica` | 60 minutos | 100 entradas | Dados históricos não se alteram |
| `dimensoes` | 2 horas | 500 entradas | Dimensões só mudam em carga do ETL |

Os TTLs são configuráveis em `application.properties` sem necessidade de recompilação.

---

## 📁 Estrutura do Projeto

```
painel-sispagaer-backend/
│
├── src/
│   ├── main/
│   │   ├── docker/
│   │   │   └── Dockerfile.jvm              # Build multi-stage, usuário não-root
│   │   │
│   │   ├── java/br/mil/fab/sispagaer/painel/
│   │   │   │
│   │   │   ├── config/
│   │   │   │   └── OpenApiConfig.java      # Definição Swagger + ApplicationPath
│   │   │   │
│   │   │   ├── security/
│   │   │   │   ├── ApiKeyFilter.java       # Autenticação serviço-a-serviço
│   │   │   │   └── SecurityHeadersFilter.java  # Headers de segurança HTTP
│   │   │   │
│   │   │   ├── resource/                   # Camada REST (endpoints)
│   │   │   │   ├── FolhaPagamentoResource.java
│   │   │   │   └── DimensaoResource.java
│   │   │   │
│   │   │   ├── service/                    # Camada de negócio (cache + resiliência)
│   │   │   │   ├── FolhaPagamentoService.java
│   │   │   │   └── DimensaoService.java
│   │   │   │
│   │   │   ├── repository/                 # Camada de dados (SQL explícito)
│   │   │   │   ├── FatoFolhaPagamentoRepository.java
│   │   │   │   └── DimensaoRepository.java
│   │   │   │
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   │   └── FiltroConsultaDTO.java   # @BeanParam com Bean Validation
│   │   │   │   └── response/                    # Records Java (imutáveis)
│   │   │   │       ├── TotalPorPatenteDTO.java
│   │   │   │       ├── TotalPorOrganizacaoDTO.java
│   │   │   │       ├── SerieTemporalDTO.java
│   │   │   │       ├── DashboardResumoDTO.java
│   │   │   │       ├── DimensaoPostoGraduacaoDTO.java
│   │   │   │       └── DimensaoOrganizacaoMilitarDTO.java
│   │   │   │
│   │   │   ├── exception/
│   │   │   │   ├── NegocioException.java       # Exceção de regra de negócio
│   │   │   │   ├── ErroResponseDTO.java        # Formato padronizado de erro
│   │   │   │   └── GlobalExceptionMapper.java  # Tratamento centralizado
│   │   │   │
│   │   │   ├── health/
│   │   │   │   └── DatabaseHealthCheck.java    # Readiness async para Kubernetes
│   │   │   │
│   │   │   └── util/
│   │   │       └── RowUtil.java                # Helper BigDecimal agnóstico ao banco
│   │   │
│   │   └── resources/
│   │       └── application.properties          # Configuração por perfil (dev/test/prod)
│   │
│   └── test/
│       └── java/br/mil/fab/sispagaer/painel/
│           └── resource/
│               └── FolhaPagamentoResourceTest.java  # Testes com @QuarkusTest + Mockito
│
├── .mvn/wrapper/
│   └── maven-wrapper.properties           # Versão do Maven (3.9.9)
├── mvnw                                   # Maven Wrapper (Unix/WSL)
├── mvnw.cmd                               # Maven Wrapper (Windows)
├── pom.xml                                # Dependências e build
├── .gitignore
└── README.md
```

---

## 📦 Pré-requisitos

| Ferramenta | Versão mínima | Observação |
|---|---|---|
| JDK | 25 | [Eclipse Temurin](https://adoptium.net/) ou [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) |
| Maven | — | Não precisa instalar — use `mvnw.cmd` (Windows) ou `./mvnw` (Unix) |
| Docker | 24+ | Necessário apenas para Dev Services e build de imagem |
| MySQL | 8.4 | Necessário apenas se não usar Dev Services |

---

## ⚙️ Configuração e Execução

### 1. Clone o repositório

```bash
git clone https://github.com/fab/painel-sispagaer-backend.git
cd painel-sispagaer-backend
```

### 2. Modo desenvolvimento (recomendado)

#### Com Dev Services (MySQL automático via Docker)

Não é necessário configurar banco de dados. O Quarkus sobe um contêiner MySQL 8.4 automaticamente:

```powershell
# Windows
mvnw.cmd quarkus:dev
```

```bash
# Unix / WSL / macOS
./mvnw quarkus:dev
```

#### Com MySQL externo (DW real ou instância local)

```powershell
mvnw.cmd quarkus:dev `
  -Dquarkus.datasource.reactive.url="mysql://localhost:3306/dw_sispagaer" `
  -Dquarkus.datasource.username="sispagaer" `
  -Dquarkus.datasource.password="sua_senha" `
  -Dsispagaer.security.api-key="dev-key"
```

### 3. URLs disponíveis em modo dev

| URL | Descrição |
|---|---|
| `http://localhost:8080/swagger-ui` | Documentação interativa da API |
| `http://localhost:8080/openapi` | Contrato OpenAPI (JSON/YAML) |
| `http://localhost:8080/q/health` | Status de saúde da aplicação |
| `http://localhost:8080/q/dev` | Dev UI do Quarkus (extensões, beans, cache...) |
| `http://localhost:8080/q/metrics` | Métricas Prometheus |

### 4. Build para produção

```bash
# Pacote JVM (fast-jar)
./mvnw package -DskipTests

# Com build da imagem Docker
./mvnw package -DskipTests -Dquarkus.container-image.build=true
```

---

## 🔧 Variáveis de Ambiente

Todas as variáveis sensíveis devem ser injetadas via ambiente — nunca armazenadas em arquivos do projeto.

| Variável | Obrigatória | Padrão (dev) | Descrição |
|---|---|---|---|
| `DB_URL` | ✅ | `mysql://localhost:3306/dw_sispagaer` | URL de conexão com o DW |
| `DB_USERNAME` | ✅ | `sispagaer` | Usuário do banco de dados |
| `DB_PASSWORD` | ✅ | `changeme` | Senha do banco de dados |
| `API_KEY` | ✅ | `dev-only-key-troque-em-producao` | Chave de autenticação do frontend |
| `CORS_ALLOWED_ORIGINS` | ✅ | `http://localhost:3000` | Origem(ns) permitida(s) pelo CORS |
| `CONTAINER_REGISTRY` | ❌ | `registry.fab.mil.br` | Registry Docker para publicação da imagem |

> Em Kubernetes, todas estas variáveis devem ser configuradas como **Secrets**, nunca como ConfigMaps.

---

## 🚀 Deploy em Kubernetes

### Gerar os manifestos

O plugin `quarkus-kubernetes` gera os manifestos automaticamente durante o build:

```bash
./mvnw package -DskipTests
# Manifestos gerados em: target/kubernetes/kubernetes.yml
```

### Configurar os Secrets

```bash
kubectl create secret generic sispagaer-secrets \
  --from-literal=DB_URL="mysql://dw-host:3306/dw_sispagaer" \
  --from-literal=DB_USERNAME="sispagaer" \
  --from-literal=DB_PASSWORD="senha-segura" \
  --from-literal=API_KEY="chave-segura-de-producao" \
  --from-literal=CORS_ALLOWED_ORIGINS="https://painel.sispagaer.fab.mil.br" \
  --namespace=sispagaer
```

### Aplicar os manifestos

```bash
kubectl apply -f target/kubernetes/kubernetes.yml -n sispagaer
```

### Configuração do cluster (valores padrão)

| Parâmetro | Valor |
|---|---|
| Namespace | `sispagaer` |
| Réplicas | `2` |
| CPU request/limit | `250m / 500m` |
| Memória request/limit | `256Mi / 512Mi` |
| Liveness probe | `GET /q/health/live` (delay 10s) |
| Readiness probe | `GET /q/health/ready` (delay 5s) |

---

## 🧪 Testes

```bash
# Executar todos os testes
./mvnw test

# Executar com relatório de cobertura (JaCoCo)
./mvnw verify

# Executar um teste específico
./mvnw test -Dtest=FolhaPagamentoResourceTest
```

Os testes usam `@QuarkusTest` com `@InjectMock` do Mockito — o banco de dados é completamente mockado, sem necessidade de infraestrutura adicional.

---

## 👨‍💻 Desenvolvedor

<div align="center">

**Daniel Chipolesch**

Desenvolvedor responsável pela concepção, arquitetura e implementação do Painel SISPAGAER Backend.

</div>

---

## 🔭 Ideias para o Futuro

As funcionalidades abaixo foram identificadas como evoluções naturais do sistema e podem ser priorizadas em sprints futuros:

### 🔐 Segurança e Governança

- [ ] **Autenticação OIDC / Keycloak** — integrar com o provedor de identidade da FAB para autenticação de usuários reais, com controle de acesso por perfil (ex: gestor nacional vs. gestor de OM)
- [ ] **Autorização por OM** — usuários com perfil de gestores de OM devem visualizar apenas os dados da própria organização
- [ ] **Auditoria de acesso** — registrar quem consultou quais dados e quando, com persistência em tabela de auditoria (governança de dados)
- [ ] **Rate limiting por cliente** — limitar o número de requisições por API Key para prevenir abuso

### 📊 Novos Endpoints e Análises

- [ ] **Totais por rubrica** — detalhar a composição da folha por tipo de vencimento/desconto (rubricas da `dim_rubrica`)
- [ ] **Análise por situação funcional** — distribuição do efetivo por situação (ativo, reserva, reformado)
- [ ] **Variação mensal** — percentual de variação da folha entre períodos consecutivos
- [ ] **Comparativo anual** — comparação da folha do período atual com o mesmo período do ano anterior
- [ ] **Exportação** — endpoints para download dos dados em CSV ou Excel para análises externas
- [ ] **Endpoint de média e mediana salarial** — indicadores estatísticos por categoria e OM

### ⚡ Performance e Escalabilidade

- [ ] **Cache distribuído com Redis** — substituir o Caffeine por Redis para compartilhar cache entre réplicas do pod, eliminando o "cache cold start" após reinicializações
- [ ] **Paginação nos endpoints de listagem** — para OMs e rubricas com grande volume de registros
- [ ] **Compressão Gzip** — comprimir respostas grandes (série histórica, ranking de OMs)
- [ ] **HTTP/2** — habilitar multiplexação de requisições para o frontend

### 🔭 Observabilidade

- [ ] **Tracing distribuído** — integrar OpenTelemetry para rastrear o ciclo de vida das requisições de ponta a ponta (frontend → backend → DW)
- [ ] **Dashboard Grafana** — criar painel de monitoramento do backend com métricas de latência, throughput e taxa de erros por endpoint
- [ ] **Alertas automáticos** — configurar alertas no Prometheus/Alertmanager para Circuit Breaker aberto e latência elevada

### 🏗️ Arquitetura

- [ ] **Build nativo (GraalVM)** — compilar para binário nativo reduzindo o tempo de startup de ~1s para ~10ms, ideal para escalabilidade horizontal rápida em Kubernetes
- [ ] **Pipeline CI/CD** — automatizar build, testes, análise de código (SonarQube) e deploy no cluster da FAB via GitLab CI ou Jenkins
- [ ] **Testes de carga** — criar suite de testes de performance com Gatling para validar o comportamento sob alta concorrência
- [ ] **Versionamento da API** — implementar estratégia de versioning (`/api/v2/`) para evoluir contratos sem quebrar clientes existentes
- [ ] **Suporte multi-banco** — abstrair a camada de repositório para suportar troca transparente entre MySQL, SQL Server e PostgreSQL via configuração

---

<div align="center">

**DIRETORIA DE ADMINISTRAÇÃO DA AERONÁUTICA**
</div>
<div align="center">

**SUBDIRETORIA DE PAGAMENTO DE PESSOAL**
</div>
