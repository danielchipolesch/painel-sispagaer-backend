package br.mil.fab.sispagaer.painel.util;

import io.vertx.mutiny.sqlclient.Row;

import java.math.BigDecimal;

/**
 * Utilitário de mapeamento de colunas do Vert.x SQL Client.
 *
 * <p>O tipo retornado por {@code row.getValue()} varia conforme o driver:
 * <ul>
 *   <li>MySQL: colunas {@code DECIMAL} retornam {@link BigDecimal}</li>
 *   <li>SQL Server: pode retornar {@link Double} em alguns drivers</li>
 * </ul>
 * Este helper trata todos os casos de forma defensiva, garantindo que dados
 * financeiros nunca percam precisão — independentemente do banco subjacente.</p>
 */
public final class RowUtil {

    private RowUtil() {}

    /**
     * Lê uma coluna numérica do resultado da query e retorna como {@link BigDecimal}.
     * Nunca retorna {@code null} — retorna {@code BigDecimal.ZERO} se o valor for nulo.
     *
     * @param row    Linha do resultado reativo
     * @param coluna Nome da coluna no resultado SQL
     * @return Valor como BigDecimal, nunca null
     */
    public static BigDecimal toBigDecimal(Row row, String coluna) {
        Object value = row.getValue(coluna);
        return switch (value) {
            case BigDecimal bd -> bd;
            case Double d      -> BigDecimal.valueOf(d);
            case Number n      -> new BigDecimal(n.toString());
            case null          -> BigDecimal.ZERO;
            default            -> BigDecimal.ZERO;
        };
    }
}
