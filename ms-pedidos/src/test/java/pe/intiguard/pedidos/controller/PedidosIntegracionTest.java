package pe.intiguard.pedidos.controller;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pe.intiguard.pedidos.client.CatalogoClient;
import pe.intiguard.pedidos.client.ComprobantesClient;
import pe.intiguard.pedidos.client.InventarioClient;
import pe.intiguard.pedidos.client.ProductoDto;
import pe.intiguard.pedidos.entity.EstadoVenta;
import pe.intiguard.pedidos.entity.Venta;
import pe.intiguard.pedidos.repository.CarritoRepository;
import pe.intiguard.pedidos.repository.VentaRepository;
import pe.intiguard.security.JwtService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Carrito y checkout (saga con compensación) contra H2, con los tres servicios simulados. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pedidos;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "intiguard.checkout.timeout-comprobante-ms=500",
        "intiguard.checkout.revisar-cada-ms=3600000",
        "intiguard.jwt.secret=clave-de-prueba-de-al-menos-32-caracteres!!",
        "intiguard.security.exigir-token=true"})
@AutoConfigureMockMvc
class PedidosIntegracionTest {

    private static final long ANA = 7L;
    private static final long LUIS = 8L;
    private static final Request PETICION = Request.create(Request.HttpMethod.POST, "/x", Map.of(), null, StandardCharsets.UTF_8, null);

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;
    @Autowired private VentaRepository ventaRepository;
    @Autowired private CarritoRepository carritoRepository;
    @MockitoBean private CatalogoClient catalogoClient;
    @MockitoBean private InventarioClient inventarioClient;
    @MockitoBean private ComprobantesClient comprobantesClient;

    @BeforeEach
    void preparar() {
        ventaRepository.deleteAll();
        carritoRepository.deleteAll();
        reset(catalogoClient, inventarioClient, comprobantesClient);
        when(catalogoClient.obtenerProducto(1L)).thenReturn(
                new ProductoDto(1L, "Cámara IP Wi-Fi 1080p", "Ezviz", new BigDecimal("159.90"), "/img/1.png", true));
        when(catalogoClient.obtenerProducto(2L)).thenReturn(
                new ProductoDto(2L, "Cámara bala exterior 4 MP", "Hikvision", new BigDecimal("289.00"), null, true));
        when(catalogoClient.obtenerProducto(3L)).thenReturn(
                new ProductoDto(3L, "Descontinuado", "Dahua", new BigDecimal("10.00"), null, false));
        when(catalogoClient.obtenerProducto(99L)).thenThrow(new FeignException.NotFound("no", PETICION, null, null));
        when(comprobantesClient.emitir(anyString(), any())).thenReturn(new ComprobantesClient.Comprobante(1L, 1L, "B001-00000001"));
    }

    private String token(long id, String rol) {
        return "Bearer " + jwtService.generarToken(id, "u" + id + "@x.pe", rol);
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String token, String cuerpo) throws Exception {
        if (token != null) peticion.header("Authorization", token);
        if (cuerpo != null) peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        return mvc.perform(peticion);
    }

    private void llenarCarrito(long usuario) throws Exception {
        enviar(post("/carrito/items"), token(usuario, "CLIENTE"), "{\"idProducto\":1,\"cantidad\":2}");
        enviar(post("/carrito/items"), token(usuario, "CLIENTE"), "{\"idProducto\":2}");
    }

    private ResultActions checkout(long usuario, String cuerpo) throws Exception {
        return enviar(post("/pedidos/checkout"), token(usuario, "CLIENTE"), cuerpo);
    }

    private static final String BOLETA = "{\"tipoComprobante\":\"BOLETA\",\"docCliente\":\"\",\"nombreRazonSocial\":\"Ana Torres\"}";

    @Test
    void elCarritoSumaCantidadesYArmaLaRespuestaConElCatalogo() throws Exception {
        String ana = token(ANA, "CLIENTE");
        enviar(get("/carrito"), ana, null).andExpect(jsonPath("$.items.length()").value(0)).andExpect(jsonPath("$.total").value(0));

        enviar(post("/carrito/items"), ana, "{\"idProducto\":1}").andExpect(status().isCreated());
        enviar(post("/carrito/items"), ana, "{\"idProducto\":1,\"cantidad\":2}")
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].cantidad").value(3))
                .andExpect(jsonPath("$.items[0].marca").value("Ezviz"))
                .andExpect(jsonPath("$.items[0].importe").value(479.7))
                .andExpect(jsonPath("$.total").value(479.7));

        enviar(put("/carrito/items/1"), ana, "{\"cantidad\":1}").andExpect(jsonPath("$.total").value(159.9));
        enviar(put("/carrito/items/2"), ana, "{\"cantidad\":1}").andExpect(status().isNotFound());
        enviar(put("/carrito/items/1"), ana, "{\"cantidad\":0}").andExpect(status().isBadRequest());
        enviar(post("/carrito/items"), ana, "{\"idProducto\":3}").andExpect(status().isNotFound());
        enviar(post("/carrito/items"), ana, "{\"idProducto\":99}").andExpect(status().isNotFound());

        enviar(delete("/carrito/items/1"), ana, null).andExpect(jsonPath("$.items.length()").value(0));
        enviar(delete("/carrito/items/1"), ana, null).andExpect(status().isOk());

        // Cada cliente tiene su carrito y el ADMIN no tiene carrito
        enviar(get("/carrito"), token(LUIS, "CLIENTE"), null).andExpect(jsonPath("$.items.length()").value(0));
        enviar(get("/carrito"), token(1, "ADMIN"), null).andExpect(status().isForbidden());
        enviar(get("/carrito"), null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void checkoutCorrectoConfirmaLaVentaYVaciaElCarrito() throws Exception {
        llenarCarrito(ANA);
        checkout(ANA, BOLETA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.idUsuario").value(ANA))
                .andExpect(jsonPath("$.total").value(608.8))
                .andExpect(jsonPath("$.subtotal").value(515.93))
                .andExpect(jsonPath("$.igv").value(92.87))
                .andExpect(jsonPath("$.detalles.length()").value(2))
                .andExpect(jsonPath("$.detalles[0].importe").value(319.8));

        verify(inventarioClient).reservar(eq(token(ANA, "CLIENTE")), any());
        verify(comprobantesClient).emitir(anyString(), argThat(e -> e.idUsuario() == ANA && e.docCliente() == null));
        verify(inventarioClient).confirmar(anyString(), any());
        verify(inventarioClient, never()).liberar(anyString(), any());
        enviar(get("/carrito"), token(ANA, "CLIENTE"), null).andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void sinStockLaVentaQuedaAnuladaYSeDevuelveElProducto() throws Exception {
        llenarCarrito(ANA);
        byte[] cuerpo = "{\"status\":409,\"mensaje\":\"Stock insuficiente\",\"detalles\":[\"idProducto: 2\"]}".getBytes(StandardCharsets.UTF_8);
        when(inventarioClient.reservar(anyString(), any())).thenThrow(new FeignException.Conflict("409", PETICION, cuerpo, null));

        checkout(ANA, BOLETA)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Stock insuficiente para Cámara bala exterior 4 MP"))
                .andExpect(jsonPath("$.detalles[0]").value("idProducto: 2"));
        assertThat(ventaRepository.findAll()).extracting(Venta::getEstado).containsExactly(EstadoVenta.ANULADA);
        verify(comprobantesClient, never()).emitir(anyString(), any());
        // El carrito se conserva para que el cliente lo corrija
        enviar(get("/carrito"), token(ANA, "CLIENTE"), null).andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void siComprobantesFallaSeLiberaElStockYLaVentaQuedaAnulada() throws Exception {
        llenarCarrito(ANA);
        when(comprobantesClient.emitir(anyString(), any())).thenThrow(new RuntimeException("ms-comprobantes caído"));

        checkout(ANA, BOLETA)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.startsWith("El servicio de comprobantes no está disponible")));
        verify(inventarioClient).liberar(anyString(), any());
        verify(inventarioClient, never()).confirmar(anyString(), any());
        assertThat(ventaRepository.findAll()).extracting(Venta::getEstado).containsExactly(EstadoVenta.ANULADA);
    }

    @Test
    void siComprobantesTardaMasDelLimiteTambienSeCompensa() throws Exception {
        llenarCarrito(ANA);
        when(comprobantesClient.emitir(anyString(), any())).thenAnswer(i -> {
            Thread.sleep(1500);
            return new ComprobantesClient.Comprobante(1L, 1L, "B001-00000001");
        });
        checkout(ANA, BOLETA).andExpect(status().isServiceUnavailable());
        verify(inventarioClient).liberar(anyString(), any());
    }

    @Test
    void validacionesDelCheckout() throws Exception {
        checkout(ANA, BOLETA).andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value("El carrito está vacío"));
        llenarCarrito(ANA);
        checkout(ANA, "{\"tipoComprobante\":\"FACTURA\",\"docCliente\":\"123\",\"nombreRazonSocial\":\"X SAC\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0]").value(org.hamcrest.Matchers.startsWith("docCliente:")));
        checkout(ANA, "{\"tipoComprobante\":\"TICKET\",\"nombreRazonSocial\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.length()").value(2));
        enviar(post("/pedidos/checkout"), token(1, "ADMIN"), BOLETA).andExpect(status().isForbidden());
        assertThat(ventaRepository.count()).isZero();
    }

    @Test
    void cadaClienteVeSusPedidosYElAdminTodos() throws Exception {
        llenarCarrito(ANA);
        String venta = checkout(ANA, BOLETA).andReturn().getResponse().getContentAsString();
        long idVenta = Long.parseLong(venta.replaceAll("^\\{\"id\":(\\d+),.*", "$1"));
        llenarCarrito(LUIS);
        checkout(LUIS, "{\"tipoComprobante\":\"FACTURA\",\"docCliente\":\"20601234567\",\"nombreRazonSocial\":\"Seguridad SAC\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoComprobante").value("FACTURA"));

        enviar(get("/pedidos"), token(ANA, "CLIENTE"), null)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].detalles.length()").value(2));
        enviar(get("/pedidos"), token(1, "ADMIN"), null).andExpect(jsonPath("$.length()").value(2));
        enviar(get("/pedidos/" + idVenta), token(LUIS, "CLIENTE"), null).andExpect(status().isForbidden());
        enviar(get("/pedidos/" + idVenta), token(ANA, "CLIENTE"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles[1].nombreProducto").value("Cámara bala exterior 4 MP"));
        enviar(get("/pedidos/999999"), token(1, "ADMIN"), null).andExpect(status().isNotFound());
    }

    @Test
    void unProductoDescontinuadoEnElCarritoFrenaElCheckout() throws Exception {
        llenarCarrito(ANA);
        when(catalogoClient.obtenerProducto(2L)).thenReturn(
                new ProductoDto(2L, "Cámara bala exterior 4 MP", "Hikvision", new BigDecimal("289.00"), null, false));
        checkout(ANA, BOLETA)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles[0]").value("idProducto: 2"));
        assertThat(ventaRepository.count()).isZero();
        verifyNoInteractions(inventarioClient);
    }
}
