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
        Venta venta = Venta.builder().idUsuario(1L).build();
        DetalleVenta detalle = DetalleVenta.builder().venta(venta).idProducto(7L).nombreProducto("Cámara IP")
                .cantidad(2).precioUnitario(new BigDecimal("199.90")).build();
        venta.getDetalles().add(detalle);
        venta.asignarMontos(detalle.getImporte());
        ventaRepository.save(venta);

        assertThat(ventaRepository.findByIdUsuarioOrderByFechaVentaDesc(1L))
                .singleElement()
                .satisfies(v -> {
                    assertThat(v.getDetalles()).hasSize(1);
                    assertThat(v.getFechaVenta()).isNotNull();
                });
    }

    @Test
    void desglosaElIgvDelTotal() {
        Venta venta = Venta.builder().idUsuario(1L).build();
        venta.asignarMontos(new BigDecimal("399.80"));

        assertThat(venta.getSubtotal()).isEqualByComparingTo("338.81");
        assertThat(venta.getIgv()).isEqualByComparingTo("60.99");
        assertThat(venta.getSubtotal().add(venta.getIgv())).isEqualByComparingTo(venta.getTotal());
    }
}
