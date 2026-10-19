package pe.intiguard.pedidos.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit breaker de la emisión del comprobante (paso 5 del checkout):
 * si ms-comprobantes tarda más de 2 s, se corta la llamada y el checkout compensa.
 * Con la mitad de las últimas 10 llamadas fallidas se abre 10 s y falla de inmediato, sin esperar.
 */
@Configuration
public class CircuitBreakerConfig {

    public static final String COMPROBANTES = "comprobantes";

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> circuitoComprobantes(
            @Value("${intiguard.checkout.timeout-comprobante-ms:2000}") long timeoutMs) {
        return factory -> factory.configure(builder -> builder
                .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(timeoutMs)).build())
                .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        .slidingWindowType(SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .build()), COMPROBANTES);
    }
}
