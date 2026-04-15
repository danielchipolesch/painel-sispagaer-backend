package br.mil.fab.sispagaer.painel.resource;

import br.mil.fab.sispagaer.painel.dto.response.DimensaoOrganizacaoMilitarDTO;
import br.mil.fab.sispagaer.painel.dto.response.DimensaoPostoGraduacaoDTO;
import br.mil.fab.sispagaer.painel.exception.ErroResponseDTO;
import br.mil.fab.sispagaer.painel.service.DimensaoService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
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
 * Endpoints de consulta às dimensões do Data Warehouse.
 *
 * <p>Usados pelo frontend para popular filtros, dropdowns e legendas.
 * Todas as respostas são fortemente cacheadas (TTL 2 horas) pois
 * dimensões só mudam em carga do ETL.</p>
 */
@Path("/dimensoes")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Dimensões")
@SecurityRequirement(name = "ApiKey")
public class DimensaoResource {

    @Inject
    DimensaoService service;

    // -----------------------------------------------------------------
    // Postos e Graduações
    // -----------------------------------------------------------------

    @GET
    @Path("/postos-graduacoes")
    @Operation(
        summary = "Lista todos os postos e graduações",
        description = "Retorna todos os postos/graduações cadastrados no DW, ordenados pela hierarquia militar."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Lista de postos/graduações",
            content = @Content(schema = @Schema(implementation = DimensaoPostoGraduacaoDTO.class))),
        @APIResponse(responseCode = "401", description = "Não autorizado",
            content = @Content(schema = @Schema(implementation = ErroResponseDTO.class)))
    })
    public Uni<List<DimensaoPostoGraduacaoDTO>> listarPostosGraduacoes(
            @QueryParam("categoria")
            @DefaultValue("")
            @Parameter(description = "Filtrar por categoria: OFICIAL, PRACA ou CIVIL. Omitir para todos.", example = "OFICIAL")
            String categoria) {

        if (!categoria.isBlank()) {
            return service.listarPostosPorCategoria(categoria);
        }
        return service.listarPostosGraduacoes();
    }

    // -----------------------------------------------------------------
    // Organizações Militares
    // -----------------------------------------------------------------

    @GET
    @Path("/organizacoes-militares")
    @Operation(
        summary = "Lista todas as Organizações Militares",
        description = """
            Retorna todas as OMs cadastradas no DW, ordenadas por sigla.
            Use o filtro `codComaer` para listar OMs de um Comando Aéreo Regional específico.
            """
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Lista de Organizações Militares",
            content = @Content(schema = @Schema(implementation = DimensaoOrganizacaoMilitarDTO.class))),
        @APIResponse(responseCode = "401", description = "Não autorizado",
            content = @Content(schema = @Schema(implementation = ErroResponseDTO.class)))
    })
    public Uni<List<DimensaoOrganizacaoMilitarDTO>> listarOrganizacoesMilitares(
            @QueryParam("codComaer")
            @DefaultValue("")
            @Parameter(description = "Código do Comando Aéreo Regional (ex: COMGAP, COMAR1, COMAE). Omitir para todos.", example = "COMGAP")
            String codComaer) {

        if (!codComaer.isBlank()) {
            return service.listarOrganizacoesPorComaer(codComaer);
        }
        return service.listarOrganizacoesMilitares();
    }
}
