package pe.intiguard.inventario.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import pe.intiguard.inventario.entity.EstadoReserva;
import pe.intiguard.inventario.entity.Reserva;
import pe.intiguard.inventario.entity.Stock;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class StockRepositoryTest {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Test
    void elStockSeBuscaPorElIdDelProducto() {
        stockRepository.save(Stock.builder().idProducto(7L).disponible(7).reservado(3).build());

        assertThat(stockRepository.findById(7L))
                .isPresent()
                .get()
                .satisfies(s -> {
                    assertThat(s.getDisponible()).isEqualTo(7);
                    assertThat(s.getReservado()).isEqualTo(3);
                });
    }

    @Test
    void encuentraSoloLasReservasActivasVencidas() {
        stockRepository.save(Stock.builder().idProducto(7L).disponible(10).build());
        LocalDateTime ahora = LocalDateTime.now();
        reservaRepository.save(reserva(1L, EstadoReserva.ACTIVA, ahora.minusMinutes(1)));
        reservaRepository.save(reserva(2L, EstadoReserva.ACTIVA, ahora.plusMinutes(10)));
        reservaRepository.save(reserva(3L, EstadoReserva.CONFIRMADA, ahora.minusMinutes(1)));

        assertThat(reservaRepository.findByEstadoAndExpiraEnBefore(EstadoReserva.ACTIVA, ahora))
                .extracting(Reserva::getIdVenta)
                .containsExactly(1L);
    }

    private Reserva reserva(Long idVenta, EstadoReserva estado, LocalDateTime expiraEn) {
        return Reserva.builder().idVenta(idVenta).idProducto(7L).cantidad(1).estado(estado).expiraEn(expiraEn).build();
    }
}
