package br.mil.fab.sispagaer.painel.service;

import br.mil.fab.sispagaer.painel.dto.response.DashboardResumoDTO;
import br.mil.fab.sispagaer.painel.dto.response.SerieTemporalDTO;
import br.mil.fab.sispagaer.painel.dto.response.TotalPorOrganizacaoDTO;
import br.mil.fab.sispagaer.painel.dto.response.TotalPorPatenteDTO;
import br.mil.fab.sispagaer.painel.exception.NegocioException;
import br.mil.fab.sispagaer.painel.repository.FatoFolhaPagamentoRepository;
import io.quarkus.cache.CacheResult;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Serviço de negócio para dados de folha de pagamento.
 *
 * <h3>Responsabilidades desta camada</h3>
 * <ol>
 *   <li><strong>Cache:</strong> {@code @CacheResult} do Quarkus Cache (Caffeine).
 *       Evita bater no DW para cada requisição. TTLs configurados em
 *       {@code application.properties}.</li>
 *   <li><strong>Fault Tolerance:</strong> {@code @Timeout}, {@code @Retry},
 *       {@code @CircuitBreaker} do MicroProfile — protegem contra lentidão do DW.</li>
 *   <li><strong>Queries paralelas:</strong> {@code Uni.combine().all()} dispara
 *       múltiplas queries simultaneamente no pool reativo.</li>
 *   <li><strong>Validação de negócio:</strong> regras que não cabem em Bean Validation.</li>
 * </ol>
 */
@ApplicationScoped
public class FolhaPagamentoService {

    private static final Logger LOG = Logger.getLogger(FolhaPagamentoService.class);

    /** Limite de OMs retornadas no ranking do dashboard. */
    private static final int TOP_OMS = 15;

    /** Meses de histórico exibidos no gráfico de série temporal. */
    private static final int MESES_HISTORICO = 24;

    @Inject
    FatoFolhaPagamentoRepository repository;

    // -----------------------------------------------------------------
    // Dashboard — API Composition com queries paralelas
    // -----------------------------------------------------------------

    /**
     * Monta o resumo completo do dashboard disparando 3 queries em paralelo.
     *
     * <p>O {@code Uni.combine().all()} do Mutiny submete as 3 queries ao pool
     * reativo simultaneamente. O resultado só é emitido quando todas concluem,
     * sem bloquear nenhuma thread da JVM (Virtual Threads + event loop).</p>
     *
     * <p>Resultado cacheado por 15 minutos (configurado em application.properties).</p>
     */
    @CacheResult(cacheName = "dashboard")
    @Timeout(value = 30, unit = ChronoUnit.SECONDS)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 30, delayUnit = ChronoUnit.SECONDS)
    @CircuitBreakerName("dashboard-dw")
    public Uni<DashboardResumoDTO> buscarResumo(int ano, int mes) {
        validarPeriodo(ano, mes);

        LOG.infof("Buscando resumo do dashboard: %d/%02d", ano, mes);

        String competencia = "%d%02d".formatted(ano, mes);

        return Uni.combine().all()
            .unis(
                repository.buscarTotalPorPatente(ano, mes, null, null),
                repository.buscarTotalPorOrganizacao(ano, mes, TOP_OMS),
                repository.buscarSerieHistorica(ano, mes, MESES_HISTORICO)
            )
            .asTuple()
            .map(tuple -> {
                var porPatente     = tuple.getItem1();
                var porOrganizacao = tuple.getItem2();
                var historico      = tuple.getItem3();

                // Totaliza a partir dos registros por posto (evita query extra)
                BigDecimal totalBruto    = somarCampo(porPatente, TotalPorPatenteDTO::valBrutoTotal);
                BigDecimal totalLiquido  = somarCampo(porPatente, TotalPorPatenteDTO::valLiquidoTotal);
                BigDecimal totalDescontos = somarCampo(porPatente, TotalPorPatenteDTO::valDescontoTotal);
                long       qtdMilitares  = porPatente.stream().mapToLong(TotalPorPatenteDTO::qtdMilitares).sum();

                return new DashboardResumoDTO(
                    competencia,
                    totalBruto,
                    totalLiquido,
                    totalDescontos,
                    qtdMilitares,
                    porPatente,
                    porOrganizacao,
                    historico
                );
            });
    }

    // -----------------------------------------------------------------
    // Totais por Posto / Graduação
    // -----------------------------------------------------------------

    /**
     * Totais de folha por posto/graduação para um período.
     *
     * <p>Cacheado por 30 minutos. A chave de cache combina ano, mes, codOm e categoria,
     * garantindo cache granular por combinação de filtros.</p>
     */
    @CacheResult(cacheName = "totais")
    @Timeout(value = 20, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS)
    public Uni<List<TotalPorPatenteDTO>> buscarTotalPorPatente(
            int ano, int mes, String codOm, String categoria) {
        validarPeriodo(ano, mes);
        return repository.buscarTotalPorPatente(ano, mes, codOm, categoria);
    }

    // -----------------------------------------------------------------
    // Totais por Organização Militar
    // -----------------------------------------------------------------

    @CacheResult(cacheName = "totais")
    @Timeout(value = 20, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS)
    public Uni<List<TotalPorOrganizacaoDTO>> buscarTotalPorOrganizacao(int ano, int mes, int limite) {
        validarPeriodo(ano, mes);
        if (limite <= 0 || limite > 200) {
            throw new NegocioException("Parâmetro 'limite' deve estar entre 1 e 200.", Response.Status.BAD_REQUEST);
        }
        return repository.buscarTotalPorOrganizacao(ano, mes, limite);
    }

    // -----------------------------------------------------------------
    // Série Histórica
    // -----------------------------------------------------------------

    @CacheResult(cacheName = "serie-historica")
    @Timeout(value = 25, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS)
    public Uni<List<SerieTemporalDTO>> buscarSerieHistorica(int anoFim, int mesFim, int meses) {
        validarPeriodo(anoFim, mesFim);
        if (meses <= 0 || meses > 120) {
            throw new NegocioException("Parâmetro 'meses' deve estar entre 1 e 120.", Response.Status.BAD_REQUEST);
        }
        return repository.buscarSerieHistorica(anoFim, mesFim, meses);
    }

    // -----------------------------------------------------------------
    // Helpers privados
    // -----------------------------------------------------------------

    private void validarPeriodo(int ano, int mes) {
        if (mes < 1 || mes > 12) {
            throw new NegocioException("Mês inválido: " + mes + ". Deve ser entre 1 e 12.");
        }
        if (ano < 2000 || ano > 2100) {
            throw new NegocioException("Ano inválido: " + ano + ".");
        }
    }

    private BigDecimal somarCampo(List<TotalPorPatenteDTO> lista,
                                   java.util.function.Function<TotalPorPatenteDTO, BigDecimal> extrator) {
        return lista.stream()
            .map(extrator)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
