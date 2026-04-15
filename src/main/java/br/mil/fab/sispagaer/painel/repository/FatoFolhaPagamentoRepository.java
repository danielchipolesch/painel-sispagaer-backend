package br.mil.fab.sispagaer.painel.repository;

import br.mil.fab.sispagaer.painel.dto.response.SerieTemporalDTO;
import br.mil.fab.sispagaer.painel.dto.response.TotalPorOrganizacaoDTO;
import br.mil.fab.sispagaer.painel.dto.response.TotalPorPatenteDTO;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Repositório do fato central do Data Warehouse: {@code fat_folha_pagamento}.
 *
 * <h3>Decisões de design para o DW</h3>
 * <ul>
 *   <li><strong>Queries explícitas:</strong> sem ORM/JPA, pois não há entidades — apenas
 *       dimensões e fatos. Isso também garante controle total sobre o plano de execução.</li>
 *   <li><strong>Reativo (Mutiny):</strong> o Vert.x MySQLPool gerencia um pool de conexões
 *       assíncronas. Queries concorrentes são disparadas sem bloquear threads da JVM.</li>
 *   <li><strong>Prepared statements:</strong> parâmetros posicionais {@code ?} (padrão MySQL)
 *       evitam SQL injection — regra inviolável de governança e segurança.</li>
 *   <li><strong>BigDecimal via RowUtil:</strong> preserva precisão de colunas DECIMAL
 *       do banco — essencial para dados financeiros de folha.</li>
 * </ul>
 *
 * <h3>Uso de queries paralelas</h3>
 * <pre>{@code
 * // Na camada de serviço, múltiplas queries disparadas simultaneamente:
 * Uni.combine().all()
 *     .unis(repo.buscarTotalPorPatente(ano, mes, null),
 *           repo.buscarTotalPorOrganizacao(ano, mes, null))
 *     .asTuple();
 * }</pre>
 */
@ApplicationScoped
public class FatoFolhaPagamentoRepository {

    private static final Logger LOG = Logger.getLogger(FatoFolhaPagamentoRepository.class);

    @Inject
    MySQLPool client;

    // -----------------------------------------------------------------
    // Totais por Posto / Graduação
    // -----------------------------------------------------------------

    /**
     * Retorna o total de folha agrupado por posto/graduação para o período informado.
     *
     * @param ano      Ano de competência
     * @param mes      Mês de competência (1–12)
     * @param codOm    Filtro opcional de Organização Militar (UASG). {@code null} = todas.
     * @param categoria Filtro opcional: OFICIAL, PRACA, CIVIL. {@code null} = todas.
     */
    public Uni<List<TotalPorPatenteDTO>> buscarTotalPorPatente(
            int ano, int mes, String codOm, String categoria) {

        // MySQL usa '?' para parâmetros posicionais (não $1, $2 como no PostgreSQL).
        // Filtros opcionais: cada '? IS NULL OR col = ?' consome 2 slots no Tuple.
        var sql = """
            SELECT
                pg.sig_posto_graduacao,
                pg.nom_posto_graduacao,
                pg.categoria,
                pg.ord_hierarquia,
                SUM(f.val_bruto)              AS val_bruto_total,
                SUM(f.val_liquido)            AS val_liquido_total,
                SUM(f.val_desconto)           AS val_desconto_total,
                COUNT(DISTINCT f.cod_militar) AS qtd_militares
            FROM fat_folha_pagamento f
            INNER JOIN dim_posto_graduacao    pg ON f.id_posto_graduacao = pg.id_posto_graduacao
            INNER JOIN dim_organizacao_militar om ON f.id_om             = om.id_om
            INNER JOIN dim_tempo               t  ON f.id_tempo          = t.id_tempo
            WHERE t.ano = ?
              AND t.mes = ?
              AND (? IS NULL OR om.cod_uasg  = ?)
              AND (? IS NULL OR pg.categoria = ?)
            GROUP BY
                pg.sig_posto_graduacao,
                pg.nom_posto_graduacao,
                pg.categoria,
                pg.ord_hierarquia
            ORDER BY pg.ord_hierarquia
            """;

        LOG.debugf("buscarTotalPorPatente: ano=%d mes=%d codOm=%s categoria=%s", ano, mes, codOm, categoria);

        // codOm e categoria são passados duas vezes: uma para IS NULL, outra para a comparação
        return client
            .preparedQuery(sql)
            .execute(Tuple.of(ano, mes, codOm, codOm, categoria, categoria))
            .onItem().transform(rows ->
                StreamSupport.stream(rows.spliterator(), false)
                    .map(TotalPorPatenteDTO::fromRow)
                    .toList()
            )
            .onFailure().invoke(ex -> LOG.errorf(ex, "Erro em buscarTotalPorPatente"));
    }

    // -----------------------------------------------------------------
    // Totais por Organização Militar
    // -----------------------------------------------------------------

    /**
     * Retorna o total de folha agrupado por Organização Militar para o período informado.
     *
     * @param ano      Ano de competência
     * @param mes      Mês de competência
     * @param limite   Número máximo de OMs retornadas (para ranking — ex: top 10)
     */
    public Uni<List<TotalPorOrganizacaoDTO>> buscarTotalPorOrganizacao(
            int ano, int mes, int limite) {

        var sql = """
            SELECT
                om.cod_uasg,
                om.sig_om,
                om.nom_om,
                om.nom_comaer,
                om.uf,
                SUM(f.val_bruto)              AS val_bruto_total,
                SUM(f.val_liquido)            AS val_liquido_total,
                SUM(f.val_desconto)           AS val_desconto_total,
                COUNT(DISTINCT f.cod_militar) AS qtd_militares
            FROM fat_folha_pagamento f
            INNER JOIN dim_organizacao_militar om ON f.id_om    = om.id_om
            INNER JOIN dim_tempo               t  ON f.id_tempo = t.id_tempo
            WHERE t.ano = ?
              AND t.mes = ?
            GROUP BY
                om.cod_uasg, om.sig_om, om.nom_om, om.nom_comaer, om.uf
            ORDER BY val_liquido_total DESC
            LIMIT ?
            """;

        LOG.debugf("buscarTotalPorOrganizacao: ano=%d mes=%d limite=%d", ano, mes, limite);

        return client
            .preparedQuery(sql)
            .execute(Tuple.of(ano, mes, limite))
            .onItem().transform(rows ->
                StreamSupport.stream(rows.spliterator(), false)
                    .map(TotalPorOrganizacaoDTO::fromRow)
                    .toList()
            )
            .onFailure().invoke(ex -> LOG.errorf(ex, "Erro em buscarTotalPorOrganizacao"));
    }

    // -----------------------------------------------------------------
    // Série Histórica
    // -----------------------------------------------------------------

    /**
     * Retorna a série histórica mensal dos últimos {@code meses} meses,
     * retroagindo a partir do mês/ano informado.
     *
     * <p>Usada para construir o gráfico de evolução temporal no painel.</p>
     *
     * @param anoFim  Ano final da série
     * @param mesFim  Mês final da série
     * @param meses   Quantidade de meses retroativos
     */
    public Uni<List<SerieTemporalDTO>> buscarSerieHistorica(int anoFim, int mesFim, int meses) {

        // Aritmética de período: (ano * 12 + mes) produz um inteiro crescente por mês.
        // Ex: Nov/2024 = 2024*12+11 = 24299. Isso evita DATE_TRUNC/MAKE_DATE do PostgreSQL,
        // que não existem no MySQL. A expressão é portável para qualquer banco SQL.
        var sql = """
            SELECT
                t.ano,
                t.mes,
                SUM(f.val_bruto)              AS val_bruto_total,
                SUM(f.val_liquido)            AS val_liquido_total,
                SUM(f.val_desconto)           AS val_desconto_total,
                COUNT(DISTINCT f.cod_militar) AS qtd_militares
            FROM fat_folha_pagamento f
            INNER JOIN dim_tempo t ON f.id_tempo = t.id_tempo
            WHERE (t.ano * 12 + t.mes) BETWEEN ((? * 12 + ?) - ? + 1) AND (? * 12 + ?)
            GROUP BY t.ano, t.mes
            ORDER BY t.ano, t.mes
            """;

        LOG.debugf("buscarSerieHistorica: anoFim=%d mesFim=%d meses=%d", anoFim, mesFim, meses);

        // anoFim e mesFim passados duas vezes: uma para o limite inferior, outra para o superior
        return client
            .preparedQuery(sql)
            .execute(Tuple.of(anoFim, mesFim, meses, anoFim, mesFim))
            .onItem().transform(rows ->
                StreamSupport.stream(rows.spliterator(), false)
                    .map(SerieTemporalDTO::fromRow)
                    .toList()
            )
            .onFailure().invoke(ex -> LOG.errorf(ex, "Erro em buscarSerieHistorica"));
    }
}
