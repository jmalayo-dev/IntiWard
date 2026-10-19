package pe.intiguard.pedidos.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import pe.intiguard.pedidos.config.CircuitBreakerConfig;

import static org.assertj.core.api.Assertions.assertThat;

/** El circuito de comprobantes espera lo configurado (2 s), no el segundo que trae Resilience4j por defecto. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:circuito;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "intiguard.jwt.secret=clave-de-prueba-de-al-menos-32-caracteres!!"})
class CircuitoComprobantesTest {

    @Autowired private CircuitBreakerFactory<?, ?> factory;

    private boolean responde(long demoraMs) {
        return factory.create(CircuitBreakerConfig.COMPROBANTES).run(() -> {
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }, error -> false);
    }

    @Test
    void esperaHastaDosSegundos() {
        assertThat(responde(1300)).isTrue();
        assertThat(responde(2600)).isFalse();
    }
}
