package pe.intiguard.comprobantes.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pe.intiguard.comprobantes.client.PedidosClient;
import pe.intiguard.comprobantes.client.VentaDto;
import pe.intiguard.comprobantes.entity.SerieCorrelativo;
import pe.intiguard.comprobantes.entity.TipoComprobante;
import pe.intiguard.comprobantes.repository.ComprobanteRepository;
import pe.intiguard.comprobantes.repository.SerieCorrelativoRepository;
import pe.intiguard.security.JwtService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Emisión, control de dueño, reporte y PDF contra H2, con ms-pedidos simulado. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:comprobantes;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "intiguard.jwt.secret=clave-de-prueba-de-al-menos-32-caracteres!!",
        "intiguard.security.exigir-token=true"})
@AutoConfigureMockMvc
class ComprobanteIntegracionTest {

    private static final long ANA = 7L;
    private static final long LUIS = 8L;

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;
    @Autowired private ComprobanteRepository comprobanteRepository;
    @Autowired private SerieCorrelativoRepository serieRepository;
    @MockitoBean private PedidosClient pedidosClient;

    @BeforeEach
    void preparar() {
        comprobanteRepository.deleteAll();
        serieRepository.deleteAll();
        serieRepository.save(SerieCorrelativo.builder().serie("B001").tipo(TipoComprobante.BOLETA).build());
        serieRepository.save(SerieCorrelativo.builder().serie("F001").tipo(TipoComprobante.FACTURA).build());
    }

    private String token(long id, String rol) {
        return "Bearer " + jwtService.generarToken(id, "u" + id + "@x.pe", rol);
    }

    private ResultActions emitir(long idVenta, long idUsuario, String tipo, String doc, String nombre) throws Exception {
        String cuerpo = """
                {"idVenta":%d,"idUsuario":%d,"tipo":"%s","docCliente":%s,"nombreRazonSocial":%s,
                 "subtotal":271.02,"igv":48.78,"total":319.80}"""
                .formatted(idVenta, idUsuario, tipo, doc == null ? "null" : "\"" + doc + "\"",
                        nombre == null ? "null" : "\"" + nombre + "\"");
        return mvc.perform(post("/comprobantes").header("Authorization", token(idUsuario, "CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private String idDe(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString().replaceAll(".*\"id\":(\\d+),.*", "$1");
    }

    @Test
    void numeraPorSerieYEsIdempotentePorVenta() throws Exception {
        emitir(1, ANA, "BOLETA", null, "Ana Torres")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("B001-00000001"))
                .andExpect(jsonPath("$.idUsuario").doesNotExist());
        emitir(2, ANA, "BOLETA", "45678912", "Ana Torres")
                .andExpect(jsonPath("$.numero").value("B001-00000002"));
        emitir(3, LUIS, "FACTURA", "20601234567", "Seguridad Integral del Sur S.A.C.")
                .andExpect(jsonPath("$.numero").value("F001-00000001"))
                .andExpect(jsonPath("$.pdfUrl").isNotEmpty());

        // La misma venta otra vez: 200 con el mismo número, sin consumir correlativo
        emitir(1, ANA, "BOLETA", null, "Ana Torres")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("B001-00000001"));
        assertThat(serieRepository.findById("B001").orElseThrow().getUltimoCorrelativo()).isEqualTo(2);
    }

    @Test
    void rechazaDocumentosInvalidos() throws Exception {
        emitir(1, ANA, "FACTURA", "12345678901", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.length()").value(2));
        emitir(2, ANA, "BOLETA", "1234", "Ana")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0]").value("docCliente: el DNI debe tener 8 dígitos"));
        assertThat(comprobanteRepository.count()).isZero();
    }

    @Test
    void cadaClienteVeSoloLosSuyosYElAdminTodos() throws Exception {
        String idAna = idDe(emitir(1, ANA, "BOLETA", null, "Ana"));
        emitir(2, LUIS, "BOLETA", null, "Luis");

        mvc.perform(get("/comprobantes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/comprobantes").header("Authorization", token(ANA, "CLIENTE")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idVenta").value(1));
        mvc.perform(get("/comprobantes").header("Authorization", token(1, "ADMIN")))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].idVenta").value(2));

        mvc.perform(get("/comprobantes/" + idAna).header("Authorization", token(LUIS, "CLIENTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/comprobantes/venta/1").header("Authorization", token(ANA, "CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("B001-00000001"));
        mvc.perform(get("/comprobantes/venta/99").header("Authorization", token(ANA, "CLIENTE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void elReporteSumaElRangoYEsSoloParaAdmin() throws Exception {
        emitir(1, ANA, "BOLETA", null, "Ana");
        emitir(2, LUIS, "BOLETA", null, "Luis");
        String hoy = LocalDate.now().toString();

        mvc.perform(get("/comprobantes/reporte").header("Authorization", token(ANA, "CLIENTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/comprobantes/reporte?desde=" + hoy + "&hasta=" + hoy).header("Authorization", token(1, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(2))
                .andExpect(jsonPath("$.total").value(639.6))
                .andExpect(jsonPath("$.desde").value(hoy));
        mvc.perform(get("/comprobantes/reporte?hasta=2000-01-01").header("Authorization", token(1, "ADMIN")))
                .andExpect(jsonPath("$.cantidad").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void elPdfSeDescargaConElNumeroComoNombre() throws Exception {
        String id = idDe(emitir(1, ANA, "BOLETA", "45678912", "Ana Torres"));
        when(pedidosClient.obtenerVenta(anyLong())).thenReturn(new VentaDto(1L, List.of(
                new VentaDto.Detalle(1L, "Cámara IP Wi-Fi 1080p", new BigDecimal("159.90"), 2, new BigDecimal("319.80")))));

        byte[] pdf = mvc.perform(get("/comprobantes/" + id + "/pdf").header("Authorization", token(ANA, "CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"B001-00000001.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");

        mvc.perform(get("/comprobantes/" + id + "/pdf").header("Authorization", token(LUIS, "CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void elPdfSaleAunqueMsPedidosNoResponda() throws Exception {
        String id = idDe(emitir(1, ANA, "BOLETA", null, "Ana"));
        when(pedidosClient.obtenerVenta(anyLong())).thenThrow(new RuntimeException("ms-pedidos caído"));
        mvc.perform(get("/comprobantes/" + id + "/pdf").header("Authorization", token(ANA, "CLIENTE")))
                .andExpect(status().isOk());
    }
}
