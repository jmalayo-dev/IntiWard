package pe.intiguard.pedidos.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import pe.intiguard.pedidos.entity.DetalleVenta;
import pe.intiguard.pedidos.entity.Venta;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class VentaRepositoryTest {

    @Autowired
    private VentaRepository ventaRepository;

    @Test
    void guardaLaVentaConSusDetallesEnCascada() {
        Venta venta = Venta.builder().idUsuario(1L).total(new BigDecimal("399.80")).build();
        venta.getDetalles().add(DetalleVenta.builder().venta(venta).idProducto(7L).nombreProducto("Cámara IP")
                .cantidad(2).precioUnitario(new BigDecimal("199.90")).subtotal(new BigDecimal("399.80")).build());
        ventaRepository.save(venta);

        assertThat(ventaRepository.findByIdUsuarioOrderByFechaDesc(1L))
                .singleElement()
                .satisfies(v -> assertThat(v.getDetalles()).hasSize(1));
    }
}
