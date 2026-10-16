package pe.intiguard.comprobantes.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import pe.intiguard.comprobantes.entity.SerieCorrelativo;
import pe.intiguard.comprobantes.entity.TipoComprobante;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class SerieCorrelativoRepositoryTest {

    @Autowired
    private SerieCorrelativoRepository serieRepository;

    @Test
    void bloqueaLaSerieDelTipoYAvanzaElCorrelativo() {
        serieRepository.save(SerieCorrelativo.builder().serie("B001").tipo(TipoComprobante.BOLETA).build());
        serieRepository.save(SerieCorrelativo.builder().serie("F001").tipo(TipoComprobante.FACTURA).build());

        SerieCorrelativo boleta = serieRepository.findFirstByTipoOrderBySerieAsc(TipoComprobante.BOLETA).orElseThrow();

        assertThat(boleta.getSerie()).isEqualTo("B001");
        assertThat(boleta.siguiente()).isEqualTo(1);
        assertThat(boleta.siguiente()).isEqualTo(2);
    }
}
