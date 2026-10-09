package pe.intiguard.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** El cliente web conoce una sola URL: la del gateway. Nunca llama a un servicio directo. */
@Configuration
public class GatewayConfig {

    @Bean
    public RestClient gatewayRestClient(@Value("${intiguard.gateway-url}") String gatewayUrl) {
        return RestClient.builder().baseUrl(gatewayUrl).build();
    }
}
