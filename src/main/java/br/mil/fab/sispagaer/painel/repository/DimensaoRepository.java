package br.mil.fab.sispagaer.painel.repository;

import br.mil.fab.sispagaer.painel.dto.response.DimensaoOrganizacaoMilitarDTO;
import br.mil.fab.sispagaer.painel.dto.response.DimensaoPostoGraduacaoDTO;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Repositório de dimensões do Data Warehouse.
 *
 * <p>Dimensões mudam raramente (apenas em carga do DW).
 * Por isso, os resultados são cacheados com TTL longo na camada de serviço,
 * reduzindo a pressão sobre o banco para consultas frequentes de filtros.</p>
 */
@ApplicationScoped
public class DimensaoRepository {

    private static final Logger LOG = Logger.getLogger(DimensaoRepository.class);

    @Inject
    MySQLPool client;

    // -----------------------------------------------------------------
    // dim_posto_graduacao
    // -----------------------------------------------------------------

    /**
     * Retorna todos os postos e graduações cadastrados no DW,
     * ordenados pela hierarquia militar.
     */
    public Uni<List<DimensaoPostoGraduacaoDTO>> buscarPostosGraduacoes() {
        var sql = """
            SELECT
                id_posto_graduacao,
                cod_posto_graduacao,
                sig_posto_graduacao,
                nom_posto_graduacao,
                categoria,
                ord_hierarquia
            FROM dim_posto_graduacao
            ORDER BY ord_hierarquia
            """;

        LOG.debug("buscarPostosGraduacoes");

        return client
            .query(sql)
            .execute()
            .onItem().transform(rows ->
                StreamSupport.stream(rows.spliterator(), false)
                    .map(DimensaoPostoGraduacaoDTO::fromRow)
                    .toList()
            )
            .onFailure().invoke(ex -> LOG.errorf(ex, "Erro em buscarPostosGraduacoes"));
    }

    /**
     * Retorna postos e graduações filtrados por categoria.
     *
     * @param categoria OFICIAL, PRACA ou CIVIL
     */
    public Uni<List<DimensaoPostoGraduacaoDTO>> buscarPostosPorCategoria(String categoria) {
        var sql = """
            SELECT
                id_posto_graduacao,
                cod_posto_graduacao,
                sig_posto_graduacao,
                nom_posto_graduacao,
                categoria,
                ord_hierarquia
            FROM dim_posto_graduacao
            WHERE categoria = ?
            ORDER BY ord_hierarquia
            """;

        return client
            .preparedQuery(sql)
            .execute(Tuple.of(categoria))
            .onItem().transform(rows ->
                StreamSupport.stream(rows.spliterator(), false)
                    .map(DimensaoPostoGraduacaoDTO::fromRow)
                    .toList()
            )
            .onFailure().invoke(ex -> LOG.errorf(ex, "Erro em buscarPostosPorCategoria: %s", categoria));
    }

    // -----------------------------------------------------------------
    // dim_organizacao_militar
    // -----------------------------------------------------------------

    /**
     * Retorna todas as Organizações Militares cadastradas no DW,
     * ordenadas por sigla.
     */
    public Uni<List<DimensaoOrganizacaoMilitarDTO>> buscarOrganizacoesMilitares() {
        var sql = """
            SELECT
                id_om,
                cod_uasg,
                sig_om,
                nom_om,
                cod_comaer,
                nom_comaer,
                uf,
                municipio
            FROM dim_organizacao_militar
            ORDER BY sig_om
            """;

        LOG.debug("buscarOrganizacoesMilitares");

        return client
            .query(sql)
            .execute()
            .onItem().transform(rows ->
                StreamSupport.stream(rows.spliterator(), false)
                    .map(DimensaoOrganizacaoMilitarDTO::fromRow)
                    .toList()
            )
            .onFailure().invoke(ex -> LOG.errorf(ex, "Erro em buscarOrganizacoesMilitares"));
    }

    /**
     * Retorna OMs filtradas por Comando Aéreo Regional (COMAER).
     *
     * @param codComaer Código do COMAER (ex: COMGAP, COMAR, COMAE)
     */
    public Uni<List<DimensaoOrganizacaoMilitarDTO>> buscarOrganizacoesPorComaer(String codComaer) {
        var sql = """
            SELECT
                id_om,
                cod_uasg,
                sig_om,
                nom_om,
                cod_comaer,
                nom_comaer,
                uf,
                municipio
            FROM dim_organizacao_militar
            WHERE cod_comaer = ?
            ORDER BY sig_om
            """;

        return client
            .preparedQuery(sql)
            .execute(Tuple.of(codComaer))
            .onItem().transform(rows ->
                StreamSupport.stream(rows.spliterator(), false)
                    .map(DimensaoOrganizacaoMilitarDTO::fromRow)
                    .toList()
            )
            .onFailure().invoke(ex -> LOG.errorf(ex, "Erro em buscarOrganizacoesPorComaer: %s", codComaer));
    }
}
