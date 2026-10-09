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
    void encuentraLaSeriePorTipoDeComprobante() {
        serieRepository.save(SerieCorrelativo.builder().tipo(TipoComprobante.BOLETA).serie("B001").ultimoCorrelativo(0).build());

        assertThat(serieRepository.findByTipo(TipoComprobante.BOLETA))
                .isPresent()
                .get().extracting(SerieCorrelativo::getSerie).isEqualTo("B001");
    }
}
