package pe.intiguard.catalogo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.intiguard.catalogo.dto.FiltroProductos;
import pe.intiguard.catalogo.dto.ProductoResponse;
import pe.intiguard.catalogo.exception.NegocioException;
import pe.intiguard.catalogo.service.ProductoService;
import pe.intiguard.security.UsuarioActual;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** GET es público (si llega un token se lee igual: inactivos=true solo vale para ADMIN). Escribir: solo ADMIN. */
@RestController
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {

    /** El panel pide todo el catálogo de una vez (tablas, gráficos, exportación): hoy son ~200 productos. */
    private static final int TAMANO_MAXIMO = 1000;

    private final ProductoService productoService;
    private final UsuarioActual usuarioActual;

    /** Página de productos. sort: precio,asc · precio,desc · nombre,asc · nombre,desc (sin sort: por id). */
    @GetMapping
    public ResponseEntity<Page<ProductoResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(defaultValue = "false") boolean conStock,
            @RequestParam(defaultValue = "false") boolean inactivos,
            @RequestParam(required = false) String sort) {
        if (page < 0 || size < 1) {
            throw NegocioException.datosInvalidos(List.of("page/size: page desde 0 y size desde 1"));
        }
        List<String> marcas = marca == null ? List.of()
                : Arrays.stream(marca.split(",")).map(String::trim).filter(m -> !m.isEmpty()).toList();
        FiltroProductos filtro = new FiltroProductos(vacioANulo(categoria), marcas, vacioANulo(q), precioMin, precioMax,
                conStock, inactivos && usuarioActual.esAdmin());
        PageRequest pagina = PageRequest.of(page, Math.min(size, TAMANO_MAXIMO), orden(sort));
        return ResponseEntity.ok(productoService.listar(filtro, pagina));
    }

    /** 200 también si está inactivo (con activo: false). 404 solo si no existe. */
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> crear(@RequestBody Map<String, Object> datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(datos));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        return ResponseEntity.ok(productoService.actualizar(id, datos));
    }

    /** Desactiva (nunca borra). */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        productoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    /** Solo precio y nombre; cualquier otro valor ordena por id (evita un 500 por un campo inexistente). */
    private static Sort orden(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by("id");
        String[] partes = sort.split(",");
        String campo = partes[0].trim();
        if (!campo.equals("precio") && !campo.equals("nombre")) return Sort.by("id");
        boolean desc = partes.length > 1 && partes[1].trim().equalsIgnoreCase("desc");
        return Sort.by(desc ? Sort.Direction.DESC : Sort.Direction.ASC, campo).and(Sort.by("id"));
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
