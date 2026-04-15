package br.mil.fab.sispagaer.painel.resource;

import br.mil.fab.sispagaer.painel.dto.response.DashboardResumoDTO;
import br.mil.fab.sispagaer.painel.dto.response.TotalPorPatenteDTO;
import br.mil.fab.sispagaer.painel.service.FolhaPagamentoService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Testes de integração dos endpoints de folha de pagamento.
 *
 * <p>Usa {@code @QuarkusTest} para subir o contexto Quarkus completo
 * e {@code @InjectMock} para substituir o serviço por um mock Mockito,
 * isolando o teste da camada de banco de dados.</p>
 *
 * <p>A API Key de teste é configurada em {@code application.properties}
 * no perfil {@code test}.</p>
 */
@QuarkusTest
class FolhaPagamentoResourceTest {

    private static final String API_KEY_TESTE  = "test-api-key-123";
    private static final String HEADER_API_KEY = "X-API-Key";

    @InjectMock
    FolhaPagamentoService service;

    // -----------------------------------------------------------------
    // Fixtures
    // -----------------------------------------------------------------

    private final TotalPorPatenteDTO totalCoronel = new TotalPorPatenteDTO(
        "CEL", "Coronel", "OFICIAL", 1,
        new BigDecimal("500000.00"),
        new BigDecimal("410000.00"),
        new BigDecimal("90000.00"),
        20L,
        new BigDecimal("20500.00")
    );

    private final DashboardResumoDTO dashboardMock = new DashboardResumoDTO(
        "202411",
        new BigDecimal("185000000.00"),
        new BigDecimal("148000000.00"),
        new BigDecimal("37000000.00"),
        68000L,
        List.of(totalCoronel),
        List.of(),
        List.of()
    );

    @BeforeEach
    void configurarMocks() {
        Mockito.when(service.buscarResumo(2024, 11))
               .thenReturn(Uni.createFrom().item(dashboardMock));

        Mockito.when(service.buscarTotalPorPatente(2024, 11, null, null))
               .thenReturn(Uni.createFrom().item(List.of(totalCoronel)));
    }

    // -----------------------------------------------------------------
    // Testes de segurança
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Deve retornar 401 quando API Key estiver ausente")
    void semApiKey_deveRetornar401() {
        given()
            .queryParam("ano", 2024)
            .queryParam("mes", 11)
        .when()
            .get("/api/v1/folha-pagamento/dashboard")
        .then()
            .statusCode(401)
            .body("status", is(401))
            .body("mensagem", containsString("ausente"));
    }

    @Test
    @DisplayName("Deve retornar 401 quando API Key for inválida")
    void comApiKeyInvalida_deveRetornar401() {
        given()
            .header(HEADER_API_KEY, "chave-errada")
            .queryParam("ano", 2024)
            .queryParam("mes", 11)
        .when()
            .get("/api/v1/folha-pagamento/dashboard")
        .then()
            .statusCode(401)
            .body("status", is(401));
    }

    // -----------------------------------------------------------------
    // Testes de funcionalidade
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Dashboard deve retornar resumo consolidado com status 200")
    void dashboard_comParametrosValidos_deveRetornar200() {
        given()
            .header(HEADER_API_KEY, API_KEY_TESTE)
            .queryParam("ano", 2024)
            .queryParam("mes", 11)
        .when()
            .get("/api/v1/folha-pagamento/dashboard")
        .then()
            .statusCode(200)
            .body("competenciaAtual", is("202411"))
            .body("qtdMilitares", is(68000))
            .body("totaisPorPatente", hasSize(1))
            .body("totaisPorPatente[0].sigPostoGraduacao", is("CEL"));
    }

    @Test
    @DisplayName("Totais por patente deve retornar lista com status 200")
    void totaisPorPatente_comParametrosValidos_deveRetornar200() {
        given()
            .header(HEADER_API_KEY, API_KEY_TESTE)
            .queryParam("ano", 2024)
            .queryParam("mes", 11)
        .when()
            .get("/api/v1/folha-pagamento/totais-por-patente")
        .then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].categoria", is("OFICIAL"))
            .body("[0].qtdMilitares", is(20));
    }

    // -----------------------------------------------------------------
    // Testes de validação de parâmetros
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Deve retornar 400 quando o mês for inválido")
    void mesInvalido_deveRetornar400() {
        given()
            .header(HEADER_API_KEY, API_KEY_TESTE)
            .queryParam("ano", 2024)
            .queryParam("mes", 13)
        .when()
            .get("/api/v1/folha-pagamento/dashboard")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("Deve retornar 400 quando o ano for omitido")
    void anoAusente_deveRetornar400() {
        given()
            .header(HEADER_API_KEY, API_KEY_TESTE)
            .queryParam("mes", 11)
        .when()
            .get("/api/v1/folha-pagamento/dashboard")
        .then()
            .statusCode(400);
    }
}
