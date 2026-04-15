package br.mil.fab.sispagaer.painel.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.stream.Collectors;

/**
 * Mapeador global de exceções: converte qualquer exceção não tratada em
 * resposta JSON padronizada ({@link ErroResponseDTO}).
 *
 * <p>Garante que stack traces e detalhes internos <strong>nunca</strong>
 * sejam expostos ao cliente — princípio de governança de dados e segurança.</p>
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable ex) {
        return switch (ex) {

            case NegocioException e -> {
                LOG.debugf("Exceção de negócio: %s", e.getMessage());
                yield Response.status(e.getStatus())
                    .entity(new ErroResponseDTO(e.getStatus().getStatusCode(), e.getMessage()))
                    .build();
            }

            case ConstraintViolationException e -> {
                String detalhes = e.getConstraintViolations().stream()
                    .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                    .collect(Collectors.joining("; "));
                LOG.debugf("Violação de validação: %s", detalhes);
                yield Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErroResponseDTO(400, "Parâmetros inválidos: " + detalhes))
                    .build();
            }

            case NotFoundException e -> Response.status(Response.Status.NOT_FOUND)
                .entity(new ErroResponseDTO(404, "Recurso não encontrado."))
                .build();

            default -> {
                // Log completo internamente, mas nunca expõe detalhes ao cliente
                LOG.errorf(ex, "Erro inesperado: %s", ex.getMessage());
                yield Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErroResponseDTO(500, "Erro interno do servidor. Contate o suporte."))
                    .build();
            }
        };
    }
}
