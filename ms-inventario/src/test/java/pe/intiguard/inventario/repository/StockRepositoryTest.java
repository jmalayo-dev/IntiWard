package pe.intiguard.inventario.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import pe.intiguard.inventario.entity.Stock;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class StockRepositoryTest {

    @Autowired
    private StockRepository stockRepository;

    @Test
    void calculaElDisponibleRestandoLoReservado() {
        stockRepository.save(Stock.builder().idProducto(7L).cantidad(10).cantidadReservada(3).build());

        assertThat(stockRepository.findByIdProducto(7L))
                .isPresent()
                .get().extracting(Stock::getDisponible).isEqualTo(7);
    }
}
