package br.mil.fab.sispagaer.painel.dto.response;

import br.mil.fab.sispagaer.painel.util.RowUtil;
import io.vertx.mutiny.sqlclient.Row;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Total de folha de pagamento agregado por Organização Militar (OM).
 *
 * <p>Produzido pela query sobre {@code fat_folha_pagamento JOIN dim_organizacao_militar}.</p>
 *
 * @param codUasg        Código UASG da OM
 * @param sigOm          Sigla da OM (ex: CGABEG, AFA, ALA1)
 * @param nomOm          Nome completo da OM
 * @param nomComaer      Comando Aéreo Regional de vinculação
 * @param uf             Unidade Federativa da sede
 * @param valLiquidoTotal Total de vencimentos líquidos
 * @param qtdMilitares   Efetivo com crédito no período
 */
@Schema(description = "Total de folha de pagamento por Organização Militar")
public record TotalPorOrganizacaoDTO(

    @Schema(example = "7236")       String codUasg,
    @Schema(example = "CGABEG")     String sigOm,
    @Schema(example = "Centro de Gerenciamento da Aviação Civil") String nomOm,
    @Schema(example = "COMGAP")     String nomComaer,
    @Schema(example = "DF")         String uf,

    @Schema(example = "4500000.00") BigDecimal valBrutoTotal,
    @Schema(example = "3800000.00") BigDecimal valLiquidoTotal,
    @Schema(example = "700000.00")  BigDecimal valDescontoTotal,

    @Schema(example = "185") long qtdMilitares

) {
    public static TotalPorOrganizacaoDTO fromRow(Row row) {
        return new TotalPorOrganizacaoDTO(
            row.getString("cod_uasg"),
            row.getString("sig_om"),
            row.getString("nom_om"),
            row.getString("nom_comaer"),
            row.getString("uf"),
            RowUtil.toBigDecimal(row, "val_bruto_total"),
            RowUtil.toBigDecimal(row, "val_liquido_total"),
            RowUtil.toBigDecimal(row, "val_desconto_total"),
            row.getLong("qtd_militares")
        );
    }
}
