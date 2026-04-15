package br.mil.fab.sispagaer.painel.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Adiciona headers de segurança HTTP a todas as respostas.
 *
 * <p>Esses headers são uma camada extra de defesa (defense-in-depth)
 * além das configurações do Ingress e do CORS.</p>
 */
@Provider
public class SecurityHeadersFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        var headers = res.getHeaders();

        // Impede que a resposta seja exibida em iframes (clickjacking)
        headers.add("X-Frame-Options", "DENY");

        // Impede sniffing de Content-Type pelo browser
        headers.add("X-Content-Type-Options", "nosniff");

        // Força HTTPS por 1 ano (HSTS) — efetivo quando há TLS no Ingress
        headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Restringe referrer a mesma origem
        headers.add("Referrer-Policy", "same-origin");

        // Desabilita features desnecessárias do browser
        headers.add("Permissions-Policy", "geolocation=(), microphone=(), camera=()");

        // Controle de cache: dados de folha são sensíveis, não devem ser cacheados pelo browser
        headers.add("Cache-Control", "no-store");
        headers.add("Pragma", "no-cache");
    }
}
