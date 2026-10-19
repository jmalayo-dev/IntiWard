package pe.intiguard.catalogo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.intiguard.catalogo.client.InventarioClient;
import pe.intiguard.catalogo.client.StockDto;
import pe.intiguard.catalogo.dto.NombreActivoResponse;
import pe.intiguard.catalogo.entity.Categoria;
import pe.intiguard.catalogo.entity.Marca;
import pe.intiguard.catalogo.exception.NegocioException;
import pe.intiguard.catalogo.exception.RecursoNoEncontradoException;
import pe.intiguard.catalogo.repository.CategoriaRepository;
import pe.intiguard.catalogo.repository.MarcaRepository;
import pe.intiguard.catalogo.repository.ProductoRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Marcas y categorías: el público ve las activas; el ADMIN, todas. Cada una lleva la cantidad
 * de productos activos. El nombre es único sin distinguir mayúsculas (409 si se repite).
 * GET /marcas además dice cuántos de esos productos tienen stock (se lo pregunta a ms-inventario).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarcaCategoriaService {

    private final MarcaRepository marcaRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final InventarioClient inventarioClient;

    public List<NombreActivoResponse> listarMarcas(boolean todas) {
        Map<Long, Long> conteo = conteo(productoRepository.contarActivosPorMarca());
        Map<Long, Long> conStock = conStockPorMarca();
        List<Marca> marcas = todas ? marcaRepository.findAllByOrderByNombreAsc() : marcaRepository.findByActivoTrueOrderByNombreAsc();
        return marcas.stream().map(m -> aResponse(m, conteo, conStock)).toList();
    }

    public List<NombreActivoResponse> listarCategorias(boolean todas) {
        Map<Long, Long> conteo = conteo(productoRepository.contarActivosPorCategoria());
        List<Categoria> categorias = todas ? categoriaRepository.findAllByOrderByNombreAsc()
                : categoriaRepository.findByActivoTrueOrderByNombreAsc();
        return categorias.stream().map(c -> aResponse(c, conteo)).toList();
    }

    @Transactional
    public NombreActivoResponse crearMarca(Map<String, Object> datos) {
        String nombre = nombreValido(datos.get("nombre"));
        if (marcaRepository.findFirstByNombreIgnoreCase(nombre).isPresent()) throw duplicado("una marca");
        Marca marca = marcaRepository.save(Marca.builder().nombre(nombre).build());
        return aResponse(marca, Map.of());
    }

    @Transactional
    public NombreActivoResponse crearCategoria(Map<String, Object> datos) {
        String nombre = nombreValido(datos.get("nombre"));
        if (categoriaRepository.findFirstByNombreIgnoreCase(nombre).isPresent()) throw duplicado("una categoría");
        Categoria categoria = categoriaRepository.save(Categoria.builder().nombre(nombre).build());
        return aResponse(categoria, Map.of());
    }

    /** { "nombre" } renombra y/o { "activo" } activa o desactiva. */
    @Transactional
    public NombreActivoResponse actualizarMarca(Long id, Map<String, Object> datos) {
        Marca marca = marcaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la marca " + id));
        if (datos.containsKey("nombre")) {
            String nombre = nombreValido(datos.get("nombre"));
            marcaRepository.findFirstByNombreIgnoreCase(nombre)
                    .filter(otra -> !otra.getId().equals(id))
                    .ifPresent(otra -> { throw duplicado("una marca"); });
            marca.setNombre(nombre);
        }
        if (datos.containsKey("activo")) marca.setActivo(activoValido(datos.get("activo")));
        marcaRepository.flush();
        return aResponse(marca, conteo(productoRepository.contarActivosPorMarca()));
    }

    @Transactional
    public NombreActivoResponse actualizarCategoria(Long id, Map<String, Object> datos) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría " + id));
        if (datos.containsKey("nombre")) {
            String nombre = nombreValido(datos.get("nombre"));
            categoriaRepository.findFirstByNombreIgnoreCase(nombre)
                    .filter(otra -> !otra.getId().equals(id))
                    .ifPresent(otra -> { throw duplicado("una categoría"); });
            categoria.setNombre(nombre);
        }
        if (datos.containsKey("activo")) categoria.setActivo(activoValido(datos.get("activo")));
        categoriaRepository.flush();
        return aResponse(categoria, conteo(productoRepository.contarActivosPorCategoria()));
    }

    private static String nombreValido(Object valor) {
        String nombre = Campos.texto(valor);
        if (nombre == null) throw NegocioException.datosInvalidos(List.of("nombre: es obligatorio"));
        if (nombre.length() > 60) throw NegocioException.datosInvalidos(List.of("nombre: admite hasta 60 caracteres"));
        return nombre;
    }

    private static boolean activoValido(Object valor) {
        if (valor instanceof Boolean activo) return activo;
        throw NegocioException.datosInvalidos(List.of("activo: debe ser true o false"));
    }

    private static NegocioException duplicado(String etiqueta) {
        return new NegocioException(HttpStatus.CONFLICT, "Ya existe " + etiqueta + " con ese nombre");
    }

    /** [idMarca → productos activos con stock], o null si ms-inventario no respondió (la lista sale igual, sin bloquear marcas). */
    private Map<Long, Long> conStockPorMarca() {
        Set<Long> ids = new HashSet<>();
        try {
            for (StockDto s : inventarioClient.listarStock()) {
                if (s.disponible() > 0) ids.add(s.idProducto());
            }
        } catch (RuntimeException ex) {
            log.warn("No se pudo consultar el stock para las marcas: {}", ex.getMessage());
            return null;
        }
        return ids.isEmpty() ? Map.of() : conteo(productoRepository.contarActivosPorMarcaEntre(ids));
    }

    private static Map<Long, Long> conteo(List<Object[]> filas) {
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] fila : filas) mapa.put((Long) fila[0], (Long) fila[1]);
        return mapa;
    }

    private static NombreActivoResponse aResponse(Marca m, Map<Long, Long> conteo) {
        return aResponse(m, conteo, null);
    }

    private static NombreActivoResponse aResponse(Marca m, Map<Long, Long> conteo, Map<Long, Long> conStock) {
        return new NombreActivoResponse(m.getId(), m.getNombre(), m.isActivo(), conteo.getOrDefault(m.getId(), 0L),
                conStock == null ? null : conStock.getOrDefault(m.getId(), 0L));
    }

    private static NombreActivoResponse aResponse(Categoria c, Map<Long, Long> conteo) {
        return new NombreActivoResponse(c.getId(), c.getNombre(), c.isActivo(), conteo.getOrDefault(c.getId(), 0L), null);
    }
}
