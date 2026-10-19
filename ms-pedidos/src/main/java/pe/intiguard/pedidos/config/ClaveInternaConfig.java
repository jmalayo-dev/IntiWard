package pe.intiguard.pedidos.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Manda la clave interna (CLAVE_INTERNA) en cada llamada Feign: ms-inventario y ms-comprobantes la exigen
 * en reservar, confirmar, liberar y emitir cuando tienen URL pública (Render). Sin clave no agrega nada.
 */
@Configuration
public class ClaveInternaConfig {

    static final String CABECERA = "X-Clave-Interna";

    @Bean
    public RequestInterceptor claveInterna(@Value("${intiguard.security.clave-interna:}") String clave) {
        return plantilla -> {
            if (StringUtils.hasText(clave)) {
                plantilla.header(CABECERA, clave);
            }
        };
    }
}
