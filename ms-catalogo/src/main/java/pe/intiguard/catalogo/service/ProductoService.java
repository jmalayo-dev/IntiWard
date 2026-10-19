package pe.intiguard.catalogo.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.intiguard.catalogo.client.InventarioClient;
import pe.intiguard.catalogo.client.StockDto;
import pe.intiguard.catalogo.dto.FiltroProductos;
import pe.intiguard.catalogo.dto.ProductoResponse;
import pe.intiguard.catalogo.entity.*;
import pe.intiguard.catalogo.exception.NegocioException;
import pe.intiguard.catalogo.exception.RecursoNoEncontradoException;
import pe.intiguard.catalogo.mapper.ProductoMapper;
import pe.intiguard.catalogo.repository.CategoriaRepository;
import pe.intiguard.catalogo.repository.MarcaRepository;
import pe.intiguard.catalogo.repository.ProductoRepository;

import java.math.BigDecimal;
import java.util.*;

/**
 * Productos. La tienda solo ve activos; un producto inactivo responde igual en GET /productos/{id}
 * (con activo: false) porque los pedidos pasados lo siguen mostrando. Nunca se borra: DELETE lo desactiva.
 * El stock no vive aquí: el filtro conStock se lo pregunta a ms-inventario.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final MarcaRepository marcaRepository;
    private final CategoriaRepository categoriaRepository;
    private final InventarioClient inventarioClient;

    public Page<ProductoResponse> listar(FiltroProductos filtro, Pageable pageable) {
        Set<Long> conStock = filtro.conStock() ? idsConStock() : null;
        return productoRepository.findAll(especificacion(filtro, conStock), pageable).map(ProductoMapper::toResponse);
    }

    public ProductoResponse obtener(Long id) {
        return ProductoMapper.toResponse(buscar(id));
    }

    @Transactional
    public ProductoResponse crear(Map<String, Object> datos) {
        Producto producto = new Producto();
        producto.setActivo(true);
        aplicar(producto, datos, true);
        return ProductoMapper.toResponse(productoRepository.save(producto));
    }

    /** Solo valida y cambia los campos que llegan: { "activo": true } basta para reactivar. */
    @Transactional
    public ProductoResponse actualizar(Long id, Map<String, Object> datos) {
        Producto producto = buscar(id);
        aplicar(producto, datos, false);
        return ProductoMapper.toResponse(productoRepository.saveAndFlush(producto));
    }

    @Transactional
    public void desactivar(Long id) {
        buscar(id).setActivo(false);
    }

    private Producto buscar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + id + " no existe"));
    }

    // ---------- Filtros ----------

    private Specification<Producto> especificacion(FiltroProductos f, Set<Long> conStock) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (!f.incluirInactivos()) {
                condiciones.add(cb.isTrue(raiz.get("activo")));
            }
            if (f.categoria() != null) {
                condiciones.add(porIdONombre(cb, raiz.get("categoria"), f.categoria()));
            }
            if (f.marcas() != null && !f.marcas().isEmpty()) {
                condiciones.add(cb.or(f.marcas().stream()
                        .map(m -> porIdONombre(cb, raiz.get("marca"), m))
                        .toArray(Predicate[]::new)));
            }
            if (f.q() != null) {
                // La collation utf8mb4_0900_ai_ci ya ignora mayúsculas y tildes; lower() cubre otras bases
                condiciones.add(cb.like(cb.lower(raiz.get("nombre")), "%" + escaparLike(f.q().toLowerCase()) + "%", '\\'));
            }
            if (f.precioMin() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(raiz.get("precio"), f.precioMin()));
            }
            if (f.precioMax() != null) {
                condiciones.add(cb.lessThanOrEqualTo(raiz.get("precio"), f.precioMax()));
            }
            if (conStock != null) {
                condiciones.add(conStock.isEmpty() ? cb.disjunction() : raiz.get("id").in(conStock));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    /** "3" busca por id; "Hikvision" por nombre exacto. */
    private static Predicate porIdONombre(jakarta.persistence.criteria.CriteriaBuilder cb,
                                          jakarta.persistence.criteria.Path<?> relacion, String valor) {
        if (valor.chars().allMatch(Character::isDigit)) {
            return cb.equal(relacion.get("id"), Long.valueOf(valor));
        }
        return cb.equal(cb.lower(relacion.get("nombre")), valor.toLowerCase());
    }

    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Set<Long> idsConStock() {
        try {
            Set<Long> ids = new HashSet<>();
            for (StockDto s : inventarioClient.listarStock()) {
                if (s.disponible() > 0) ids.add(s.idProducto());
            }
            return ids;
        } catch (RuntimeException ex) {
            log.warn("No se pudo consultar el stock en ms-inventario: {}", ex.getMessage());
            throw new NegocioException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo consultar el stock en este momento. Intenta sin el filtro \"Solo con stock\"");
        }
    }

    // ---------- Crear y editar ----------

    private void aplicar(Producto p, Map<String, Object> datos, boolean nuevo) {
        List<String> detalles = new ArrayList<>();

        if (nuevo || datos.containsKey("nombre")) {
            String nombre = Campos.texto(datos.get("nombre"));
            if (nombre == null) detalles.add("nombre: es obligatorio");
            else if (nombre.length() > 120) detalles.add("nombre: admite hasta 120 caracteres");
            else p.setNombre(nombre);
        }
        if (datos.containsKey("descripcion")) {
            p.setDescripcion(Campos.texto(datos.get("descripcion")));
        }
        if (datos.containsKey("imagenUrl")) {
            String url = Campos.texto(datos.get("imagenUrl"));
            if (url != null && url.length() > 255) detalles.add("imagenUrl: admite hasta 255 caracteres");
            else p.setImagenUrl(url);
        }
        if (nuevo || datos.containsKey("precio")) {
            BigDecimal precio = Campos.decimal(datos.get("precio"));
            if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) detalles.add("precio: debe ser mayor que 0");
            else if (precio.scale() > 2 || precio.precision() - precio.scale() > 8) detalles.add("precio: máximo 8 enteros y 2 decimales");
            else p.setPrecio(precio);
        }
        if (nuevo || datos.containsKey("idMarca")) {
            Long id = Campos.entero(datos.get("idMarca"));
            Optional<Marca> marca = id == null ? Optional.empty() : marcaRepository.findById(id);
            if (marca.isEmpty()) detalles.add("idMarca: no existe");
            else p.setMarca(marca.get());
        }
        if (nuevo || datos.containsKey("idCategoria")) {
            Long id = Campos.entero(datos.get("idCategoria"));
            Optional<Categoria> categoria = id == null ? Optional.empty() : categoriaRepository.findById(id);
            if (categoria.isEmpty()) detalles.add("idCategoria: no existe");
            else p.setCategoria(categoria.get());
        }
        if (datos.containsKey("activo") && datos.get("activo") != null) {
            if (datos.get("activo") instanceof Boolean activo) p.setActivo(activo);
            else detalles.add("activo: debe ser true o false");
        }
        if (datos.containsKey("especificaciones")) {
            List<String[]> specs = leerEspecificaciones(datos.get("especificaciones"), detalles);
            if (specs != null && detalles.isEmpty()) reemplazarEspecificaciones(p, specs);
        }
        if (!detalles.isEmpty()) {
            throw NegocioException.datosInvalidos(detalles);
        }
    }

    private static List<String[]> leerEspecificaciones(Object valor, List<String> detalles) {
        if (valor == null) return List.of();
        if (!(valor instanceof List<?> lista)) {
            detalles.add("especificaciones: debe ser una lista");
            return null;
        }
        List<String[]> specs = new ArrayList<>();
        Set<String> nombres = new HashSet<>();
        for (Object item : lista) {
            String nombre = item instanceof Map<?, ?> m ? Campos.texto(m.get("nombre")) : null;
            String val = item instanceof Map<?, ?> m ? Campos.texto(m.get("valor")) : null;
            if (nombre == null || val == null) {
                detalles.add("especificaciones: cada una necesita nombre y valor");
                return null;
            }
            if (nombre.length() > 60 || val.length() > 120) {
                detalles.add("especificaciones: el nombre admite 60 caracteres y el valor 120");
                return null;
            }
            if (!nombres.add(nombre.toLowerCase())) {
                detalles.add("especificaciones: hay nombres repetidos");
                return null;
            }
            specs.add(new String[]{nombre, val});
        }
        return specs;
    }

    /**
     * La lista enviada reemplaza a la anterior. Las que conservan el nombre se actualizan en su lugar:
     * así nunca se inserta un nombre que todavía existe (UNIQUE id_producto, nombre).
     */
    private static void reemplazarEspecificaciones(Producto p, List<String[]> specs) {
        Map<String, ProductoEspecificacion> actuales = new HashMap<>();
        for (ProductoEspecificacion e : p.getEspecificaciones()) {
            actuales.put(e.getNombre().toLowerCase(), e);
        }
        List<ProductoEspecificacion> nuevas = new ArrayList<>();
        for (int i = 0; i < specs.size(); i++) {
            String[] s = specs.get(i);
            ProductoEspecificacion e = actuales.remove(s[0].toLowerCase());
            if (e == null) {
                e = ProductoEspecificacion.builder().producto(p).build();
            }
            e.setNombre(s[0]);
            e.setValor(s[1]);
            e.setOrden(i);
            nuevas.add(e);
        }
        p.getEspecificaciones().clear();
        p.getEspecificaciones().addAll(nuevas);
    }
}
