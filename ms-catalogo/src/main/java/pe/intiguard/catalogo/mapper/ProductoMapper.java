package pe.intiguard.catalogo.mapper;

import pe.intiguard.catalogo.dto.ProductoResponse;
import pe.intiguard.catalogo.entity.Producto;

public final class ProductoMapper {

    private ProductoMapper() {
    }

    public static ProductoResponse toResponse(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(), p.getImagenUrl(),
                p.getMarca().getNombre(), p.getCategoria().getNombre());
    }
}
