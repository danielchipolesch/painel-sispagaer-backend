package br.mil.fab.sispagaer.painel.exception;

import jakarta.ws.rs.core.Response;

/**
 * Exceção de regra de negócio com mapeamento direto para um status HTTP.
 *
 * <p>Lance esta exceção nas camadas de serviço quando uma condição de negócio
 * não for atendida (ex: parâmetro inválido, período sem dados).
 * O {@link GlobalExceptionMapper} a converte automaticamente em resposta JSON.</p>
 *
 * <pre>{@code
 * if (mes < 1 || mes > 12) {
 *     throw new NegocioException("Mês inválido: " + mes, Response.Status.BAD_REQUEST);
 * }
 * }</pre>
 */
public class NegocioException extends RuntimeException {

    private final Response.Status status;

    public NegocioException(String mensagem, Response.Status status) {
        super(mensagem);
        this.status = status;
    }

    public NegocioException(String mensagem) {
        this(mensagem, Response.Status.BAD_REQUEST);
    }

    public Response.Status getStatus() {
        return status;
    }
}
