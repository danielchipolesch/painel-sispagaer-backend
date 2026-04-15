package br.mil.fab.sispagaer.painel.resource;

import br.mil.fab.sispagaer.painel.dto.request.FiltroConsultaDTO;
import br.mil.fab.sispagaer.painel.dto.response.DashboardResumoDTO;
import br.mil.fab.sispagaer.painel.dto.response.SerieTemporalDTO;
import br.mil.fab.sispagaer.painel.dto.response.TotalPorOrganizacaoDTO;
import br.mil.fab.sispagaer.painel.dto.response.TotalPorPatenteDTO;
import br.mil.fab.sispagaer.painel.exception.ErroResponseDTO;
import br.mil.fab.sispagaer.painel.service.FolhaPagamentoService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * Endpoints de consulta ao fato de folha de pagamento ({@code fat_folha_pagamento}).
 *
 * <p>Todos os endpoints são somente leitura (GET) e requerem autenticação
 * via header {@code X-API-Key}.</p>
 *
 * <p>Respostas são reativas ({@code Uni<T>}) — o Quarkus RESTEasy Reactive
 * serializa automaticamente para JSON sem bloquear threads.</p>
 */
@Path("/folha-pagamento")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Folha de Pagamento")
@SecurityRequirement(name = "ApiKey")
public class FolhaPagamentoResource {

    @Inject
    FolhaPagamentoService service;

    // -----------------------------------------------------------------
    // Dashboard
    // -----------------------------------------------------------------

    @GET
    @Path("/dashboard")
    @Operation(
        summary = "Resumo consolidado do dashboard",
        description = """
            Retorna todos os KPIs principais do painel em uma única requisição,
            combinando totais por posto, ranking de OMs e série histórica.

            Os dados são consultados em paralelo no Data Warehouse e cacheados
            por 15 minutos. Ideal para a carga inicial do painel.
            """
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Dashboard consolidado retornado com sucesso",
            content = @Content(schema = @Schema(implementation = DashboardResumoDTO.class))),
        @APIResponse(responseCode = "400", description = "Parâmetros inválidos",
            content = @Content(schema = @Schema(implementation = ErroResponseDTO.class))),
        @APIResponse(responseCode = "401", description = "API Key ausente ou inválida",
            content = @Content(schema = @Schema(implementation = ErroResponseDTO.class))),
        @APIResponse(responseCode = "500", description = "Erro interno do servidor",
            content = @Content(schema = @Schema(implementation = ErroResponseDTO.class)))
    })
    public Uni<DashboardResumoDTO> dashboard(@Valid @BeanParam FiltroConsultaDTO filtro) {
        return service.buscarResumo(filtro.ano, filtro.mes);
    }

    // -----------------------------------------------------------------
    // Totais por Posto / Graduação
    // -----------------------------------------------------------------

    @GET
    @Path("/totais-por-patente")
    @Operation(
        summary = "Totais de folha por posto/graduação",
        description = """
            Retorna valores bruto, líquido, descontos e efetivo agrupados por
            posto/graduação para o período de competência informado.

            Use os filtros `codOm` e `categoria` para análises segmentadas.
            """
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Totais por posto retornados com sucesso",
            content = @Content(schema = @Schema(implementation = TotalPorPatenteDTO.class))),
        @APIResponse(responseCode = "400", description = "Parâmetros inválidos",
            content = @Content(schema = @Schema(implementation = ErroResponseDTO.class))),
        @APIResponse(responseCode = "401", description = "Não autorizado",
            content = @Content(schema = @Schema(implementation = ErroResponseDTO.class)))
    })
    public Uni<List<TotalPorPatenteDTO>> totaisPorPatente(@Valid @BeanParam FiltroConsultaDTO filtro) {
        return service.buscarTotalPorPatente(filtro.ano, filtro.mes, filtro.codOm, filtro.categoria);
    }

    // -----------------------------------------------------------------
    // Totais por Organização Militar
    // -----------------------------------------------------------------

    @GET
    @Path("/totais-por-om")
    @Operation(
        summary = "Totais de folha por Organização Militar",
        description = """
            Retorna o ranking de Organizações Militares por valor total de folha
            no período informado, ordenado pelo maior valor líquido.
            """
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Totais por OM retornados com sucesso",
            content = @Content(schema = @Schema(implementation = TotalPorOrganizacaoDTO.class))),
        @APIResponse(responseCode = "400", description = "Parâmetros inválidos",
            content = @Content(schema = @Schema(implementation = ErroResponseDTO.class))),
        @APIResponse(responseCode = "401", description = "Não autorizado",
            content = @Content(schema = @Schema(implementation = ErroResponseDTO.class)))
    })
    public Uni<List<TotalPorOrganizacaoDTO>> totaisPorOrganizacao(
            @Valid @BeanParam FiltroConsultaDTO filtro,
            @QueryParam("limite")
            @DefaultValue("15")
            @Min(1) @Max(200)
            @Parameter(description = "Máximo de OMs retornadas (padrão: 15)", example = "15")
            int limite) {
        return service.buscarTotalPorOrganizacao(filtro.ano, filtro.mes, limite);
    }

    // -----------------------------------------------------------------
    // Série Histórica
    // -----------------------------------------------------------------

    @GET
    @Path("/serie-historica")
    @Operation(
        summary = "Série histórica mensal de folha",
        description = """
            Retorna a evolução mensal da folha de pagamento retroagindo N meses
            a partir do período informado. Usado para gráficos de linha/área no painel.

            Máximo de 120 meses (10 anos).
            """
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Série histórica retornada com sucesso",
            content = @Content(schema = @Schema(implementation = SerieTemporalDTO.class))),
        @APIResponse(responseCode = "400", description = "Parâmetros inválidos",
            content = @Content(schema = @Schema(implementation = ErroResponseDTO.class))),
        @APIResponse(responseCode = "401", description = "Não autorizado",
            content = @Content(schema = @Schema(implementation = ErroResponseDTO.class)))
    })
    public Uni<List<SerieTemporalDTO>> serieHistorica(
            @Valid @BeanParam FiltroConsultaDTO filtro,
            @QueryParam("meses")
            @DefaultValue("24")
            @Min(1) @Max(120)
            @Parameter(description = "Quantidade de meses retroativos (padrão: 24)", example = "24")
            int meses) {
        return service.buscarSerieHistorica(filtro.ano, filtro.mes, meses);
    }
}
