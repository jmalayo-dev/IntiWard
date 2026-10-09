package pe.intiguard.catalogo.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import pe.intiguard.catalogo.entity.Categoria;
import pe.intiguard.catalogo.entity.Marca;
import pe.intiguard.catalogo.entity.Producto;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ProductoRepositoryTest {

    @Autowired private ProductoRepository productoRepository;
    @Autowired private MarcaRepository marcaRepository;
    @Autowired private CategoriaRepository categoriaRepository;

    @Test
    void listaSoloLosProductosActivosDeUnaCategoria() {
        Marca dahua = marcaRepository.save(Marca.builder().nombre("Dahua").build());
        Categoria camaras = categoriaRepository.save(Categoria.builder().nombre("Cámaras").build());
        productoRepository.save(producto("Cámara IP 4MP", dahua, camaras, true));
        productoRepository.save(producto("Cámara descontinuada", dahua, camaras, false));

        assertThat(productoRepository.findByCategoriaIdAndActivoTrue(camaras.getId()))
                .extracting(Producto::getNombre)
                .containsExactly("Cámara IP 4MP");
    }

    private Producto producto(String nombre, Marca marca, Categoria categoria, boolean activo) {
        return Producto.builder().nombre(nombre).precio(new BigDecimal("199.90"))
                .marca(marca).categoria(categoria).activo(activo).build();
    }
}
