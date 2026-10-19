package pe.intiguard.pedidos.dto;

import java.math.BigDecimal;
import java.util.List;

/** Nombre, marca, precio e imagen se piden a ms-catalogo cada vez: el carrito solo guarda producto y cantidad. */
public record CarritoResponse(List<Item> items, BigDecimal total) {

    public record Item(Long idProducto, String nombre, String marca, BigDecimal precio, String imagenUrl, int cantidad,
                       BigDecimal importe) {
    }
}
