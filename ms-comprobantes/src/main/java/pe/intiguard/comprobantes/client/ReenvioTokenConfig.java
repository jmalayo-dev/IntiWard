package pe.intiguard.comprobantes.client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Reenvía el header Authorization de la petición en curso a las llamadas Feign. */
@Configuration
public class ReenvioTokenConfig {

    @Bean
    public RequestInterceptor reenviarToken() {
        return plantilla -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
                String token = atributos.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (token != null) {
                    plantilla.header(HttpHeaders.AUTHORIZATION, token);
                }
            }
        };
    }
}
