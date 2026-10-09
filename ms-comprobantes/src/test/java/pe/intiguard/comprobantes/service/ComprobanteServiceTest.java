package pe.intiguard.comprobantes.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComprobanteServiceTest {

    @Test
    void formateaLaSerieConOchoDigitosDeCorrelativo() {
        assertThat(ComprobanteService.formatearNumero("B001", 1)).isEqualTo("B001-00000001");
        assertThat(ComprobanteService.formatearNumero("F001", 1234)).isEqualTo("F001-00001234");
    }
}
