package br.mil.fab.sispagaer.painel.dto.response;

import br.mil.fab.sispagaer.painel.util.RowUtil;
import io.vertx.mutiny.sqlclient.Row;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Total de folha de pagamento agregado por posto/graduação.
 *
 * <p>Produzido pela query sobre {@code fat_folha_pagamento JOIN dim_posto_graduacao},
 * agrupando por posto/graduação para um dado período de competência.</p>
 *
 * @param sigPostoGraduacao  Sigla do posto/graduação (ex: CEL, MAJ, CB)
 * @param nomPostoGraduacao  Nome completo (ex: Coronel, Major, Cabo)
 * @param categoria          OFICIAL, PRACA ou CIVIL
 * @param ordHierarquia      Ordem hierárquica para ordenação no frontend
 * @param valBrutoTotal      Soma dos valores brutos no período
 * @param valLiquidoTotal    Soma dos valores líquidos no período
 * @param valDescontoTotal   Soma dos descontos no período
 * @param qtdMilitares       Quantidade de militares distintos com crédito no período
 * @param mediaSalarial      Média salarial líquida por militar
 */
@Schema(description = "Total de folha de pagamento por posto/graduação")
public record TotalPorPatenteDTO(

    @Schema(example = "CEL") String sigPostoGraduacao,
    @Schema(example = "Coronel") String nomPostoGraduacao,
    @Schema(example = "OFICIAL") String categoria,
    @Schema(example = "1") int ordHierarquia,

    @Schema(example = "1250000.00") BigDecimal valBrutoTotal,
    @Schema(example = "980000.00")  BigDecimal valLiquidoTotal,
    @Schema(example = "270000.00")  BigDecimal valDescontoTotal,

    @Schema(example = "42") long qtdMilitares,
    @Schema(example = "23333.33") BigDecimal mediaSalarial

) {
    /**
     * Constrói um DTO a partir de uma linha do resultado reativo do MySQL.
     *
     * <p>Delega a conversão DECIMAL→BigDecimal para {@link RowUtil#toBigDecimal},
     * que é agnóstico ao banco de dados.</p>
     */
    public static TotalPorPatenteDTO fromRow(Row row) {
        long qtd = row.getLong("qtd_militares");
        BigDecimal liquido = RowUtil.toBigDecimal(row, "val_liquido_total");
        BigDecimal media = qtd > 0
            ? liquido.divide(BigDecimal.valueOf(qtd), 2, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return new TotalPorPatenteDTO(
            row.getString("sig_posto_graduacao"),
            row.getString("nom_posto_graduacao"),
            row.getString("categoria"),
            row.getInteger("ord_hierarquia"),
            RowUtil.toBigDecimal(row, "val_bruto_total"),
            liquido,
            RowUtil.toBigDecimal(row, "val_desconto_total"),
            qtd,
            media
        );
    }
}
