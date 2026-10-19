package pe.intiguard.comprobantes.pdf;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class NumeroALetrasTest {

    @Test
    void escribeElImporteComoEnElComprobante() {
        assertThat(NumeroALetras.soles(new BigDecimal("578.00"))).isEqualTo("QUINIENTOS SETENTA Y OCHO CON 00/100 SOLES");
        assertThat(NumeroALetras.soles(new BigDecimal("319.8"))).isEqualTo("TRESCIENTOS DIECINUEVE CON 80/100 SOLES");
        assertThat(NumeroALetras.soles(new BigDecimal("100"))).isEqualTo("CIEN CON 00/100 SOLES");
        assertThat(NumeroALetras.soles(new BigDecimal("1199.00"))).isEqualTo("MIL CIENTO NOVENTA Y NUEVE CON 00/100 SOLES");
        assertThat(NumeroALetras.soles(new BigDecimal("21000.5"))).isEqualTo("VEINTIÚN MIL CON 50/100 SOLES");
        assertThat(NumeroALetras.soles(new BigDecimal("0.99"))).isEqualTo("CERO CON 99/100 SOLES");
        assertThat(NumeroALetras.soles(new BigDecimal("1000000"))).isEqualTo("UN MILLÓN CON 00/100 SOLES");
    }
}
