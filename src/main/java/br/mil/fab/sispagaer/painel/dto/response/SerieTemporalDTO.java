package br.mil.fab.sispagaer.painel.dto.response;

import br.mil.fab.sispagaer.painel.util.RowUtil;
import io.vertx.mutiny.sqlclient.Row;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Ponto de uma série histórica mensal de folha de pagamento.
 *
 * <p>Utilizado para construir gráficos de evolução temporal no painel.
 * Cada instância representa um mês de competência.</p>
 *
 * @param ano              Ano de competência
 * @param mes              Mês de competência (1–12)
 * @param competenciaLabel Rótulo formatado para exibição (ex: "Nov/2024")
 * @param valBrutoTotal    Total bruto do mês
 * @param valLiquidoTotal  Total líquido do mês
 * @param qtdMilitares     Efetivo com crédito no mês
 */
@Schema(description = "Ponto da série histórica mensal de folha de pagamento")
public record SerieTemporalDTO(

    @Schema(example = "2024")       int ano,
    @Schema(example = "11")         int mes,
    @Schema(example = "Nov/2024")   String competenciaLabel,

    @Schema(example = "180000000.00") BigDecimal valBrutoTotal,
    @Schema(example = "145000000.00") BigDecimal valLiquidoTotal,
    @Schema(example = "35000000.00")  BigDecimal valDescontoTotal,

    @Schema(example = "67842") long qtdMilitares

) {
    public static SerieTemporalDTO fromRow(Row row) {
        int ano = row.getInteger("ano");
        int mes = row.getInteger("mes");
        return new SerieTemporalDTO(
            ano,
            mes,
            formatarLabel(ano, mes),
            RowUtil.toBigDecimal(row, "val_bruto_total"),
            RowUtil.toBigDecimal(row, "val_liquido_total"),
            RowUtil.toBigDecimal(row, "val_desconto_total"),
            row.getLong("qtd_militares")
        );
    }

    private static String formatarLabel(int ano, int mes) {
        String[] meses = {"Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez"};
        return meses[mes - 1] + "/" + ano;
    }

    // toBigDecimal delegado para RowUtil — agnóstico ao banco de dados
}
