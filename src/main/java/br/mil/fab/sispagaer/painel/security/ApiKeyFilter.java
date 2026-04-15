package br.mil.fab.sispagaer.painel.security;

import br.mil.fab.sispagaer.painel.exception.ErroResponseDTO;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Filtro de segurança que exige API Key em todas as requisições aos endpoints
 * de negócio da aplicação.
 *
 * <h3>Estratégia de segurança (sem login de usuário)</h3>
 * <ul>
 *   <li>O frontend envia o header {@code X-API-Key} em cada requisição.</li>
 *   <li>A chave é configurada via variável de ambiente {@code API_KEY},
 *       armazenada como Secret no Kubernetes — nunca em texto plano.</li>
 *   <li>O tráfego entre frontend e backend deve trafegar exclusivamente
 *       sobre HTTPS, com terminação TLS no Ingress do Kubernetes.</li>
 *   <li>CORS está configurado para aceitar apenas a origem do frontend.</li>
 * </ul>
 *
 * <h3>Caminhos públicos (não exigem autenticação)</h3>
 * <ul>
 *   <li>/q/health — health checks para o Kubernetes</li>
 *   <li>/q/metrics — métricas para o Prometheus</li>
 *   <li>/openapi — contrato OpenAPI</li>
 *   <li>/swagger-ui — interface Swagger (desabilitar em produção se necessário)</li>
 * </ul>
 */
@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(ApiKeyFilter.class);

    @ConfigProperty(name = "sispagaer.security.api-key")
    String apiKeyEsperada;

    @ConfigProperty(name = "sispagaer.security.api-key-header", defaultValue = "X-API-Key")
    String nomeHeader;

    @ConfigProperty(name = "sispagaer.security.public-paths",
                    defaultValue = "/q/health,/q/metrics,/openapi,/swagger-ui")
    String caminhoPublicosConfig;

    @Override
    public void filter(ContainerRequestContext ctx) {
        // Preflight CORS — nunca bloquear
        if ("OPTIONS".equalsIgnoreCase(ctx.getMethod())) {
            return;
        }

        String path = ctx.getUriInfo().getPath();

        // Caminhos públicos não exigem autenticação
        List<String> caminhoPublicos = Arrays.asList(caminhoPublicosConfig.split(","));
        if (caminhoPublicos.stream().anyMatch(path::startsWith)) {
            return;
        }

        String chaveRecebida = ctx.getHeaderString(nomeHeader);

        if (chaveRecebida == null || chaveRecebida.isBlank()) {
            LOG.warnf("Requisição sem API Key para: %s %s", ctx.getMethod(), path);
            abortarNaoAutorizado(ctx, "Header X-API-Key ausente ou vazio.");
            return;
        }

        // Comparação em tempo constante para evitar timing attacks
        if (!constantTimeEquals(apiKeyEsperada, chaveRecebida)) {
            LOG.warnf("API Key inválida para: %s %s | IP de origem: %s",
                ctx.getMethod(), path,
                ctx.getHeaderString("X-Forwarded-For"));
            abortarNaoAutorizado(ctx, "API Key inválida.");
        }
    }

    private void abortarNaoAutorizado(ContainerRequestContext ctx, String mensagem) {
        ctx.abortWith(
            Response.status(Response.Status.UNAUTHORIZED)
                .header("WWW-Authenticate", "ApiKey realm=\"SISPAGAER\"")
                .entity(new ErroResponseDTO(401, mensagem))
                .build()
        );
    }

    /**
     * Compara duas strings em tempo constante para mitigar timing attacks.
     * Não usa {@code .equals()} diretamente, pois retorna mais rápido em
     * caso de divergência precoce, o que pode vazar informação sobre a chave.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int resultado = 0;
        for (int i = 0; i < a.length(); i++) {
            resultado |= a.charAt(i) ^ b.charAt(i);
        }
        return resultado == 0;
    }
}
