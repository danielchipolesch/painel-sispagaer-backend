package br.mil.fab.sispagaer.painel.config;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Configura o contrato OpenAPI (Swagger) da aplicação.
 *
 * <p>A autenticação é feita via API Key no header {@code X-API-Key},
 * configurada no filtro {@link br.mil.fab.sispagaer.painel.security.ApiKeyFilter}.
 * Não há autenticação de usuário final nesta versão.</p>
 *
 * <p>Acesse a documentação em: {@code /swagger-ui}</p>
 */
@OpenAPIDefinition(
    info = @Info(
        title = "Painel SISPAGAER — API",
        version = "1.0.0",
        description = """
            API de dados de folha de pagamento do COMAER.

            Fornece endpoints de consulta ao Data Warehouse (DW) de pessoal,
            alimentando o painel analítico do SISPAGAER.

            **Autenticação:** todas as requisições devem incluir o header
            `X-API-Key` com a chave configurada no backend.
            """,
        contact = @Contact(
            name = "Diretoria de Administração da Aeronáutica — DIRAD",
            email = "ouvidoria.dirad@fab.mil.br"
        ),
        license = @License(
            name = "Uso restrito — FAB",
            url = "https://www.fab.mil.br"
        )
    ),
    security = @SecurityRequirement(name = "ApiKey"),
    components = @Components(
        securitySchemes = @SecurityScheme(
            securitySchemeName = "ApiKey",
            type = SecuritySchemeType.APIKEY,
            description = "Chave de API para autenticação serviço-a-serviço. Envie no header X-API-Key.",
            apiKeyName = "X-API-Key",
            in = SecuritySchemeIn.HEADER
        )
    ),
    tags = {
        @Tag(name = "Folha de Pagamento", description = "Consultas ao fato central do DW: fat_folha_pagamento"),
        @Tag(name = "Dimensões",          description = "Consultas às dimensões do Data Warehouse (postos, OMs, rubricas, tempo)")
    }
)
@ApplicationPath("/api/v1")
public class OpenApiConfig extends Application {
    // Classe de configuração — sem lógica de negócio.
}


