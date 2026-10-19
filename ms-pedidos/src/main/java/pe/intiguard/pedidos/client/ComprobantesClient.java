package pe.intiguard.pedidos.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;

/** Contrato C7 con ms-comprobantes: idempotente por idVenta, debe responder en menos de 2 s. */
@FeignClient(name = "ms-comprobantes")
public interface ComprobantesClient {

    @PostMapping("/comprobantes")
    Comprobante emitir(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody EmitirRequest request);

    record EmitirRequest(Long idVenta, Long idUsuario, String tipo, String docCliente, String nombreRazonSocial,
                         BigDecimal subtotal, BigDecimal igv, BigDecimal total) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Comprobante(Long id, Long idVenta, String numero) {
    }
}
