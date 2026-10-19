package pe.intiguard.comprobantes.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/** Copia local de lo que devuelve ms-pedidos: solo lo que se imprime en el PDF. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VentaDto(Long id, List<Detalle> detalles) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Detalle(Long idProducto, String nombreProducto, BigDecimal precioUnitario, int cantidad, BigDecimal importe) {
    }
}
