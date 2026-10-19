package pe.intiguard.catalogo.controller;

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
import pe.intiguard.catalogo.client.InventarioClient;
import pe.intiguard.catalogo.client.StockDto;
import pe.intiguard.catalogo.entity.Categoria;
import pe.intiguard.catalogo.entity.Marca;
import pe.intiguard.catalogo.entity.Producto;
import pe.intiguard.catalogo.repository.CategoriaRepository;
import pe.intiguard.catalogo.repository.MarcaRepository;
import pe.intiguard.catalogo.repository.ProductoRepository;
import pe.intiguard.security.JwtService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Catálogo de punta a punta contra H2, con ms-inventario simulado. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalogo;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "intiguard.jwt.secret=clave-de-prueba-de-al-menos-32-caracteres!!",
        "intiguard.security.exigir-token=true"})
@AutoConfigureMockMvc
class CatalogoIntegracionTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private MarcaRepository marcaRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @MockitoBean private InventarioClient inventarioClient;

    private Marca hikvision;
    private Marca dahua;
    private Categoria camaras;
    private Categoria alarmas;
    private Producto camaraIp;
    private Producto descontinuada;

    @BeforeEach
    void preparar() {
        productoRepository.deleteAll();
        marcaRepository.deleteAll();
        categoriaRepository.deleteAll();
        hikvision = marcaRepository.save(Marca.builder().nombre("Hikvision").build());
        dahua = marcaRepository.save(Marca.builder().nombre("Dahua").build());
        marcaRepository.save(Marca.builder().nombre("Vieja").activo(false).build());
        camaras = categoriaRepository.save(Categoria.builder().nombre("Cámaras").build());
        alarmas = categoriaRepository.save(Categoria.builder().nombre("Alarmas").build());
        camaraIp = productoRepository.save(producto("Cámara IP Wi-Fi 1080p", "159.90", hikvision, camaras, true));
        productoRepository.save(producto("Cámara bala exterior 4 MP", "289.00", dahua, camaras, true));
        productoRepository.save(producto("Alarma Wi-Fi para hogar", "349.90", hikvision, alarmas, true));
        descontinuada = productoRepository.save(producto("Cámara descontinuada", "99.00", dahua, camaras, false));
    }

    private Producto producto(String nombre, String precio, Marca marca, Categoria categoria, boolean activo) {
        return Producto.builder().nombre(nombre).precio(new BigDecimal(precio)).marca(marca).categoria(categoria)
                .activo(activo).build();
    }

    private String admin() {
        return "Bearer " + jwtService.generarToken(1L, "admin@x.pe", "ADMIN");
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String token, String cuerpo) throws Exception {
        if (token != null) peticion.header("Authorization", token);
        if (cuerpo != null) peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        return mvc.perform(peticion);
    }

    @Test
    void listaPaginadaSoloConActivosEnFormatoViaDto() throws Exception {
        mvc.perform(get("/productos?size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].idMarca").value(hikvision.getId()))
                .andExpect(jsonPath("$.content[0].activo").value(true));
    }

    @Test
    void filtrosCombinadosYOrden() throws Exception {
        mvc.perform(get("/productos?categoria=Cámaras&marca=Dahua," + hikvision.getId()))
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mvc.perform(get("/productos?categoria=" + alarmas.getId()))
                .andExpect(jsonPath("$.content[0].nombre").value("Alarma Wi-Fi para hogar"));
        mvc.perform(get("/productos?q=cámara&precioMin=200"))
                .andExpect(jsonPath("$.content[*].nombre", contains("Cámara bala exterior 4 MP")));
        mvc.perform(get("/productos?sort=precio,desc"))
                .andExpect(jsonPath("$.content[*].precio", contains(349.9, 289.0, 159.9)));
        mvc.perform(get("/productos?sort=campoInexistente")).andExpect(status().isOk());
    }

    @Test
    void unaMarcaInactivaOcultaSusProductosDeLaTienda() throws Exception {
        dahua.setActivo(false);
        marcaRepository.save(dahua);
        mvc.perform(get("/productos")).andExpect(jsonPath("$.page.totalElements").value(2));
        enviar(get("/productos?inactivos=true"), admin(), null).andExpect(jsonPath("$.page.totalElements").value(4));
    }

    @Test
    void conStockPreguntaAInventario() throws Exception {
        when(inventarioClient.listarStock()).thenReturn(List.of(new StockDto(camaraIp.getId(), 5), new StockDto(99L, 0)));
        mvc.perform(get("/productos?conStock=true"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(camaraIp.getId()));

        when(inventarioClient.listarStock()).thenThrow(new RuntimeException("caído"));
        mvc.perform(get("/productos?conStock=true")).andExpect(status().isServiceUnavailable());
    }

    @Test
    void inactivosSoloParaAdminYElDetalleLosMuestraIgual() throws Exception {
        mvc.perform(get("/productos?inactivos=true")).andExpect(jsonPath("$.page.totalElements").value(3));
        enviar(get("/productos?inactivos=true"), admin(), null).andExpect(jsonPath("$.page.totalElements").value(4));

        mvc.perform(get("/productos/" + descontinuada.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(get("/productos/999999")).andExpect(status().isNotFound());
    }

    @Test
    void crudDeProductosConEspecificaciones() throws Exception {
        String cuerpo = """
                {"nombre":"Cámara domo 2MP","precio":199.9,"idMarca":%d,"idCategoria":%d,
                 "especificaciones":[{"nombre":"Resolución","valor":"1080p"},{"nombre":"IR","valor":"30 m"}]}"""
                .formatted(dahua.getId(), camaras.getId());
        enviar(post("/productos"), null, cuerpo).andExpect(status().isUnauthorized());
        enviar(post("/productos"), "Bearer " + jwtService.generarToken(2L, "c@x.pe", "CLIENTE"), cuerpo)
                .andExpect(status().isForbidden());

        String creado = enviar(post("/productos"), admin(), cuerpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.marca").value("Dahua"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.especificaciones[1].nombre").value("IR"))
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(creado.replaceAll("^\\{\"id\":(\\d+),.*", "$1"));

        // Reemplazar: se conserva "Resolución" (cambia el valor y el orden) y se quita "IR"
        enviar(put("/productos/" + id), admin(), """
                {"precio":"179.90","especificaciones":[{"nombre":"Lente","valor":"2.8 mm"},{"nombre":"resolución","valor":"2K"}]}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(179.9))
                .andExpect(jsonPath("$.nombre").value("Cámara domo 2MP"))
                .andExpect(jsonPath("$.especificaciones.length()").value(2))
                .andExpect(jsonPath("$.especificaciones[0].nombre").value("Lente"))
                .andExpect(jsonPath("$.especificaciones[1].valor").value("2K"));

        enviar(delete("/productos/" + id), admin(), null).andExpect(status().isNoContent());
        assertThat(productoRepository.findById(id).orElseThrow().isActivo()).isFalse();
        enviar(put("/productos/" + id), admin(), "{\"activo\":true}").andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void validacionesDelProducto() throws Exception {
        enviar(post("/productos"), admin(), """
                {"nombre":"","precio":0,"idMarca":999,"idCategoria":%d,
                 "especificaciones":[{"nombre":"A","valor":"1"},{"nombre":"a","valor":"2"}]}""".formatted(camaras.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", contains("nombre: es obligatorio", "precio: debe ser mayor que 0",
                        "idMarca: no existe", "especificaciones: hay nombres repetidos")));
    }

    @Test
    void marcasYCategoriasConContador() throws Exception {
        mvc.perform(get("/marcas"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Dahua"))
                .andExpect(jsonPath("$[0].productos").value(1))
                .andExpect(jsonPath("$[1].productos").value(2));
        enviar(get("/marcas"), admin(), null).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/categorias")).andExpect(jsonPath("$[?(@.nombre=='Cámaras')].productos").value(2));

        enviar(post("/marcas"), admin(), "{\"nombre\":\"hikvision\"}").andExpect(status().isConflict());
        enviar(post("/marcas"), admin(), "{\"nombre\":\" \"}").andExpect(status().isBadRequest());
        enviar(post("/categorias"), admin(), "{\"nombre\":\"Videoporteros\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.productos").value(0));

        enviar(put("/marcas/" + dahua.getId()), admin(), "{\"nombre\":\"Dahua Technology\",\"activo\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Dahua Technology"))
                .andExpect(jsonPath("$.activo").value(false));
        enviar(put("/categorias/" + alarmas.getId()), admin(), "{\"nombre\":\"cámaras\"}").andExpect(status().isConflict());
        enviar(put("/categorias/999"), admin(), "{\"activo\":true}").andExpect(status().isNotFound());
    }
}
