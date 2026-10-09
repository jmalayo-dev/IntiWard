package pe.intiguard.web.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogoApi {

    private final RestClient gatewayRestClient;

    /** Si el gateway o el catálogo no responden, la página muestra un aviso en vez de caerse. */
    public List<ProductoView> listarProductos() {
        try {
            return gatewayRestClient.get().uri("/productos")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException ex) {
            log.warn("No se pudo obtener el catálogo: {}", ex.getMessage());
            return List.of();
        }
    }
}
