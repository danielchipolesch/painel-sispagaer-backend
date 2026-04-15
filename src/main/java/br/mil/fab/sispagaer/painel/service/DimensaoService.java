package br.mil.fab.sispagaer.painel.service;

import br.mil.fab.sispagaer.painel.dto.response.DimensaoOrganizacaoMilitarDTO;
import br.mil.fab.sispagaer.painel.dto.response.DimensaoPostoGraduacaoDTO;
import br.mil.fab.sispagaer.painel.repository.DimensaoRepository;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Serviço de dimensões do Data Warehouse.
 *
 * <p>Dimensões são estáveis (mudam somente em carga do DW) e têm volume pequeno.
 * Por isso, o TTL de cache é longo (2 horas, configurado em application.properties),
 * aliviando completamente a pressão sobre o banco para consultas de filtros do frontend.</p>
 */
@ApplicationScoped
public class DimensaoService {

    @Inject
    DimensaoRepository repository;

    // -----------------------------------------------------------------
    // Postos e Graduações
    // -----------------------------------------------------------------

    /**
     * Lista todos os postos e graduações.
     * Cache de 2 horas — dimensão muda somente em carga do ETL.
     */
    @CacheResult(cacheName = "dimensoes")
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 3, delay = 300, delayUnit = ChronoUnit.MILLIS)
    public Uni<List<DimensaoPostoGraduacaoDTO>> listarPostosGraduacoes() {
        return repository.buscarPostosGraduacoes();
    }

    /**
     * Lista postos e graduações filtrados por categoria.
     *
     * @param categoria OFICIAL, PRACA ou CIVIL
     */
    @CacheResult(cacheName = "dimensoes")
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    public Uni<List<DimensaoPostoGraduacaoDTO>> listarPostosPorCategoria(String categoria) {
        return repository.buscarPostosPorCategoria(categoria.toUpperCase());
    }

    // -----------------------------------------------------------------
    // Organizações Militares
    // -----------------------------------------------------------------

    /**
     * Lista todas as Organizações Militares.
     * Cache de 2 horas — dimensão muda somente em carga do ETL.
     */
    @CacheResult(cacheName = "dimensoes")
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 3, delay = 300, delayUnit = ChronoUnit.MILLIS)
    public Uni<List<DimensaoOrganizacaoMilitarDTO>> listarOrganizacoesMilitares() {
        return repository.buscarOrganizacoesMilitares();
    }

    /**
     * Lista OMs por Comando Aéreo Regional.
     *
     * @param codComaer Código do COMAER (ex: COMGAP, COMAR1, COMAE)
     */
    @CacheResult(cacheName = "dimensoes")
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    public Uni<List<DimensaoOrganizacaoMilitarDTO>> listarOrganizacoesPorComaer(String codComaer) {
        return repository.buscarOrganizacoesPorComaer(codComaer.toUpperCase());
    }
}
