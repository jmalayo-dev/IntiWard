package pe.intiguard.pedidos.config;

/**
 * Circuit breaker de la emisión del comprobante (paso 5 del checkout). Se configura en application.yml
 * (resilience4j.timelimiter / resilience4j.circuitbreaker, instancia "comprobantes"):
 * si ms-comprobantes tarda más de 2 s se corta la llamada, y con la mitad de las últimas 10 llamadas
 * fallidas el circuito se abre 10 s y falla de inmediato, sin esperar.
 */
public final class CircuitBreakerConfig {

    public static final String COMPROBANTES = "comprobantes";

    private CircuitBreakerConfig() {
    }
}
