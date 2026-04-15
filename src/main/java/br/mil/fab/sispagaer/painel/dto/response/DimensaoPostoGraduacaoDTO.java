package br.mil.fab.sispagaer.painel.dto.response;

import io.vertx.mutiny.sqlclient.Row;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Dimensão de posto e graduação do Data Warehouse.
 *
 * <p>Representa a tabela {@code dim_posto_graduacao}.
 * Usada para popular filtros e legendas no frontend.</p>
 *
 * @param idPostoGraduacao  Chave surrogate da dimensão
 * @param codPostoGraduacao Código natural do posto/graduação
 * @param sigPostoGraduacao Sigla (ex: CEL, TC, MAJ, CB, SD)
 * @param nomPostoGraduacao Nome completo
 * @param categoria         OFICIAL, PRACA ou CIVIL
 * @param ordHierarquia     Posição na hierarquia militar (1 = mais alto)
 */
@Schema(description = "Dimensão posto/graduação do Data Warehouse")
public record DimensaoPostoGraduacaoDTO(

    @Schema(example = "1")        long idPostoGraduacao,
    @Schema(example = "101")      String codPostoGraduacao,
    @Schema(example = "CEL")      String sigPostoGraduacao,
    @Schema(example = "Coronel")  String nomPostoGraduacao,
    @Schema(example = "OFICIAL")  String categoria,
    @Schema(example = "1")        int ordHierarquia

) {
    public static DimensaoPostoGraduacaoDTO fromRow(Row row) {
        return new DimensaoPostoGraduacaoDTO(
            row.getLong("id_posto_graduacao"),
            row.getString("cod_posto_graduacao"),
            row.getString("sig_posto_graduacao"),
            row.getString("nom_posto_graduacao"),
            row.getString("categoria"),
            row.getInteger("ord_hierarquia")
        );
    }
}
