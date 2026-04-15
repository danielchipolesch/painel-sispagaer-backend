package br.mil.fab.sispagaer.painel.dto.response;

import io.vertx.mutiny.sqlclient.Row;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Dimensão de Organização Militar (OM) do Data Warehouse.
 *
 * <p>Representa a tabela {@code dim_organizacao_militar}.
 * Usada para popular filtros e mapas no frontend.</p>
 *
 * @param idOm       Chave surrogate da dimensão
 * @param codUasg    Código UASG (código natural de integração com SIAFI)
 * @param sigOm      Sigla da OM
 * @param nomOm      Nome completo
 * @param codComaer  Código do Comando Aéreo Regional
 * @param nomComaer  Nome do Comando Aéreo Regional
 * @param uf         Unidade Federativa da sede
 * @param municipio  Município sede
 */
@Schema(description = "Dimensão Organização Militar do Data Warehouse")
public record DimensaoOrganizacaoMilitarDTO(

    @Schema(example = "1")                                    long idOm,
    @Schema(example = "7236")                                 String codUasg,
    @Schema(example = "CGABEG")                               String sigOm,
    @Schema(example = "Centro de Gerenciamento da Aviação Civil") String nomOm,
    @Schema(example = "COMGAP")                               String codComaer,
    @Schema(example = "Comando Aéreo Pessoal")                String nomComaer,
    @Schema(example = "DF")                                   String uf,
    @Schema(example = "Brasília")                             String municipio

) {
    public static DimensaoOrganizacaoMilitarDTO fromRow(Row row) {
        return new DimensaoOrganizacaoMilitarDTO(
            row.getLong("id_om"),
            row.getString("cod_uasg"),
            row.getString("sig_om"),
            row.getString("nom_om"),
            row.getString("cod_comaer"),
            row.getString("nom_comaer"),
            row.getString("uf"),
            row.getString("municipio")
        );
    }
}
