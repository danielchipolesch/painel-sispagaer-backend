package br.mil.fab.sispagaer.painel.health;

import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Health check de readiness para o banco de dados do DW.
 *
 * <p>O Kubernetes usa este endpoint ({@code /q/health/ready}) para determinar
 * se o pod está pronto para receber tráfego. Se o DW estiver inacessível,
 * o pod é removido do balanceador até que a conectividade seja restaurada.</p>
 *
 * <p>Usa {@link AsyncHealthCheck} para não bloquear threads durante a verificação.</p>
 */
@Readiness
@ApplicationScoped
public class DatabaseHealthCheck implements AsyncHealthCheck {

    @Inject
    MySQLPool client;

    @Override
    public Uni<HealthCheckResponse> call() {
        return client
            .query("SELECT 1")
            .execute()
            .map(rows -> HealthCheckResponse.named("data-warehouse-mysql")
                .up()
                .withData("pool", "reativo")
                .withData("driver", "quarkus-reactive-mysql-client")
                .build())
            .onFailure().recoverWithItem(ex ->
                HealthCheckResponse.named("data-warehouse-mysql")
                    .down()
                    .withData("erro", ex.getMessage())
                    .build()
            );
    }
}
