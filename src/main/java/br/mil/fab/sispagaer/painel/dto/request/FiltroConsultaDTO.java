package br.mil.fab.sispagaer.painel.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

/**
 * Filtro de consulta para endpoints de folha de pagamento.
 *
 * <p>Usado como parâmetros de query string nos endpoints REST.
 * A validação é feita pelo Hibernate Validator antes de chegar à camada de serviço.</p>
 *
 * <p>Exemplo de URL:
 * {@code GET /api/v1/folha-pagamento/totais-por-patente?ano=2024&mes=11&codOm=7236}</p>
 */
public class FiltroConsultaDTO {

    @QueryParam("ano")
    @NotNull(message = "O ano de competência é obrigatório.")
    @Min(value = 2000, message = "Ano de competência inválido. Mínimo: 2000.")
    @Max(value = 2100, message = "Ano de competência inválido. Máximo: 2100.")
    @Parameter(description = "Ano de competência da folha", example = "2024", required = true)
    public Integer ano;

    @QueryParam("mes")
    @NotNull(message = "O mês de competência é obrigatório.")
    @Min(value = 1, message = "Mês inválido. Deve ser entre 1 e 12.")
    @Max(value = 12, message = "Mês inválido. Deve ser entre 1 e 12.")
    @Parameter(description = "Mês de competência da folha (1–12)", example = "11", required = true)
    public Integer mes;

    @QueryParam("codOm")
    @Parameter(description = "Código UASG da Organização Militar (opcional). Omitir para todas as OMs.", example = "7236")
    public String codOm;

    @QueryParam("categoria")
    @Parameter(description = "Categoria de pessoal: OFICIAL, PRACA ou CIVIL. Omitir para todas.", example = "OFICIAL")
    public String categoria;
}
