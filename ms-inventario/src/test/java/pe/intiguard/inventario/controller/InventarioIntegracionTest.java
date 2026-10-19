package pe.intiguard.inventario.controller;

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
import pe.intiguard.inventario.client.CatalogoClient;
import pe.intiguard.inventario.entity.EstadoReserva;
import pe.intiguard.inventario.entity.Reserva;
import pe.intiguard.inventario.entity.Stock;
import pe.intiguard.inventario.repository.MovimientoStockRepository;
import pe.intiguard.inventario.repository.ReservaRepository;
import pe.intiguard.inventario.repository.StockRepository;
import pe.intiguard.inventario.service.StockService;
import pe.intiguard.security.JwtService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Stock, movimientos y reservas del checkout contra H2, con ms-catalogo simulado. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:inventario;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "intiguard.reserva.revisar-cada-ms=3600000",
        "intiguard.jwt.secret=clave-de-prueba-de-al-menos-32-caracteres!!",
        "intiguard.security.exigir-token=true"})
@AutoConfigureMockMvc
class InventarioIntegracionTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;
    @Autowired private StockRepository stockRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private MovimientoStockRepository movimientoRepository;
    @Autowired private StockService stockService;
    @MockitoBean private CatalogoClient catalogoClient;

    @BeforeEach
    void preparar() {
        movimientoRepository.deleteAll();
        reservaRepository.deleteAll();
        stockRepository.deleteAll();
        stockRepository.save(Stock.builder().idProducto(1L).disponible(10).build());
        stockRepository.save(Stock.builder().idProducto(2L).disponible(3).build());
    }

    private String admin() {
        return "Bearer " + jwtService.generarToken(1L, "admin@x.pe", "ADMIN");
    }

    private String cliente() {
        return "Bearer " + jwtService.generarToken(5L, "ana@x.pe", "CLIENTE");
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String token, String cuerpo) throws Exception {
        if (token != null) peticion.header("Authorization", token);
        if (cuerpo != null) peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        return mvc.perform(peticion);
    }

    private Stock stock(long id) {
        return stockRepository.findById(id).orElseThrow();
    }

    @Test
    void consultasPublicasConStockEnCeroSiNoHayFila() throws Exception {
        mvc.perform(get("/stock/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(10))
                .andExpect(jsonPath("$.actualizadoEn").isNotEmpty());
        mvc.perform(get("/stock/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(0))
                .andExpect(jsonPath("$.actualizadoEn").value(nullValue()));
        mvc.perform(get("/stock?ids=2,7,1"))
                .andExpect(jsonPath("$[*].idProducto", contains(2, 7, 1)))
                .andExpect(jsonPath("$[*].disponible", contains(3, 0, 10)));
        mvc.perform(get("/stock")).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void entradaYAjusteDejanSuMovimiento() throws Exception {
        String entrada = "{\"idProducto\":1,\"cantidad\":5,\"motivo\":\"Compra a proveedor\"}";
        enviar(post("/stock/entrada"), cliente(), entrada).andExpect(status().isForbidden());
        enviar(post("/stock/entrada"), admin(), entrada)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(15));

        enviar(put("/stock/1"), admin(), "{\"disponible\":12,\"motivo\":\"Conteo físico\"}")
                .andExpect(jsonPath("$.disponible").value(12));
        // Mismo valor: no se registra un movimiento en 0
        enviar(put("/stock/1"), admin(), "{\"disponible\":12,\"motivo\":\"Conteo físico\"}").andExpect(status().isOk());
        enviar(put("/stock/1"), admin(), "{\"disponible\":-1,\"motivo\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.length()").value(2));

        enviar(get("/movimientos?idProducto=1"), admin(), null)
                .andExpect(jsonPath("$[*].tipo", contains("AJUSTE", "ENTRADA")))
                .andExpect(jsonPath("$[0].cantidad").value(-3))
                .andExpect(jsonPath("$[0].motivo").value("Conteo físico"));
        enviar(get("/movimientos"), cliente(), null).andExpect(status().isForbidden());
    }

    @Test
    void laPrimeraEntradaCreaLaFilaSiElProductoExisteEnCatalogo() throws Exception {
        when(catalogoClient.obtenerProducto(9L)).thenReturn(new CatalogoClient.Producto(9L, "Sensor", true));
        enviar(post("/stock/entrada"), admin(), "{\"idProducto\":9,\"cantidad\":4}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(4));

        Request peticion = Request.create(Request.HttpMethod.GET, "/productos/99", Map.of(), null, StandardCharsets.UTF_8, null);
        when(catalogoClient.obtenerProducto(99L)).thenThrow(new FeignException.NotFound("no", peticion, null, null));
        enviar(post("/stock/entrada"), admin(), "{\"idProducto\":99,\"cantidad\":4}").andExpect(status().isNotFound());
        assertThat(stockRepository.findById(99L)).isEmpty();
    }

    @Test
    void reservarEsTodoONadaYDevuelveElProductoQueFallo() throws Exception {
        enviar(post("/stock/reservar"), cliente(), """
                {"idVenta":42,"items":[{"idProducto":1,"cantidad":2},{"idProducto":2,"cantidad":5}]}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles[0]").value("idProducto: 2"));
        assertThat(stock(1).getDisponible()).isEqualTo(10);
        assertThat(reservaRepository.count()).isZero();

        // Producto sin fila de stock: también 409
        enviar(post("/stock/reservar"), cliente(), "{\"idVenta\":43,\"items\":[{\"idProducto\":7,\"cantidad\":1}]}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles[0]").value("idProducto: 7"));
    }

    @Test
    void reservarConfirmarYLiberarMuevenElStockUnaSolaVez() throws Exception {
        String reserva = "{\"idVenta\":42,\"items\":[{\"idProducto\":1,\"cantidad\":2},{\"idProducto\":2,\"cantidad\":3}]}";
        enviar(post("/stock/reservar"), cliente(), reserva)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idVenta").value(42))
                .andExpect(jsonPath("$.expiraEn").isNotEmpty());
        enviar(post("/stock/reservar"), cliente(), reserva).andExpect(status().isOk());
        assertThat(stock(1).getDisponible()).isEqualTo(8);
        assertThat(stock(1).getReservado()).isEqualTo(2);

        enviar(post("/stock/confirmar"), cliente(), "{\"idVenta\":42}").andExpect(status().isOk());
        enviar(post("/stock/confirmar"), cliente(), "{\"idVenta\":42}").andExpect(status().isOk());
        enviar(post("/stock/liberar"), cliente(), "{\"idVenta\":42}").andExpect(status().isOk());
        assertThat(stock(1).getDisponible()).isEqualTo(8);
        assertThat(stock(1).getReservado()).isZero();
        assertThat(stock(2).getDisponible()).isZero();

        // Otra venta que se libera: el stock vuelve
        enviar(post("/stock/reservar"), cliente(), "{\"idVenta\":50,\"items\":[{\"idProducto\":1,\"cantidad\":4}]}");
        enviar(post("/stock/liberar"), cliente(), "{\"idVenta\":50}").andExpect(status().isOk());
        assertThat(stock(1).getDisponible()).isEqualTo(8);
        assertThat(stock(1).getReservado()).isZero();
        enviar(get("/movimientos?idProducto=1"), admin(), null)
                .andExpect(jsonPath("$[*].tipo", contains("LIBERACION", "RESERVA", "CONFIRMACION", "RESERVA")));
    }

    @Test
    void laTareaDeVencimientoLiberaLasReservasVencidas() {
        Stock s = stock(1);
        s.setDisponible(8);
        s.setReservado(2);
        stockRepository.save(s);
        reservaRepository.save(Reserva.builder().idVenta(60L).idProducto(1L).cantidad(2)
                .expiraEn(LocalDateTime.now().minusMinutes(1)).build());

        assertThat(stockService.liberarVencidas()).isEqualTo(1);
        assertThat(stock(1).getDisponible()).isEqualTo(10);
        assertThat(stock(1).getReservado()).isZero();
        assertThat(reservaRepository.findByIdVenta(60L).get(0).getEstado()).isEqualTo(EstadoReserva.LIBERADA);
    }
}
