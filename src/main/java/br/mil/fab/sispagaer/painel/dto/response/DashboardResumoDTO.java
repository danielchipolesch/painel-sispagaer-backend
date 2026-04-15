package br.mil.fab.sispagaer.painel.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO agregado do dashboard principal.
 *
 * <p>Consolida, em uma única resposta, os principais indicadores do painel,
 * evitando múltiplas requisições HTTP do frontend (API composition pattern).
 * Os dados são consultados em paralelo na camada de serviço usando
 * {@code Uni.combine().all()}.</p>
 *
 * @param competenciaAtual     Período de referência (formato: "AAAAMM", ex: "202411")
 * @param totalBruto           Total bruto da folha no período
 * @param totalLiquido         Total líquido da folha no período
 * @param totalDescontos       Total de descontos no período
 * @param qtdMilitares         Efetivo total com crédito no período
 * @param totaisPorPatente     Totais agrupados por posto/graduação
 * @param totaisPorOrganizacao Totais agrupados por OM (top 10 por valor)
 * @param serieHistorica       Últimos N meses para o gráfico de evolução
 */
@Schema(description = "Resumo consolidado do dashboard principal — todos os KPIs em uma requisição")
public record DashboardResumoDTO(

    @Schema(example = "202411")          String competenciaAtual,

    @Schema(example = "185000000.00")    BigDecimal totalBruto,
    @Schema(example = "148000000.00")    BigDecimal totalLiquido,
    @Schema(example = "37000000.00")     BigDecimal totalDescontos,
    @Schema(example = "68000")           long qtdMilitares,

    List<TotalPorPatenteDTO>     totaisPorPatente,
    List<TotalPorOrganizacaoDTO> totaisPorOrganizacao,
    List<SerieTemporalDTO>       serieHistorica

) {}
