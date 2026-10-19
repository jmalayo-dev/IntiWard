package pe.intiguard.comprobantes.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.intiguard.comprobantes.dto.ComprobanteResponse;
import pe.intiguard.comprobantes.dto.EmisionResultado;
import pe.intiguard.comprobantes.dto.EmitirComprobanteRequest;
import pe.intiguard.comprobantes.dto.ReporteResponse;
import pe.intiguard.comprobantes.entity.Comprobante;
import pe.intiguard.comprobantes.mapper.ComprobanteMapper;
import pe.intiguard.comprobantes.pdf.ComprobantePdfService;
import pe.intiguard.comprobantes.service.ComprobanteService;

import java.time.LocalDate;
import java.util.List;

/**
 * Todas las rutas exigen sesión. POST /comprobantes es interna: el gateway la bloquea (403)
 * y solo la llama ms-pedidos dentro de la red.
 */
@RestController
@RequestMapping("/comprobantes")
@RequiredArgsConstructor
public class ComprobanteController {

    private final ComprobanteService comprobanteService;
    private final ComprobantePdfService pdfService;

    /** 201 nuevo · 200 si la venta ya tenía comprobante · 400 DNI/RUC inválido. */
    @PostMapping
    public ResponseEntity<ComprobanteResponse> emitir(@Valid @RequestBody EmitirComprobanteRequest request) {
        EmisionResultado resultado = comprobanteService.emitir(request);
        return ResponseEntity.status(resultado.nuevo() ? HttpStatus.CREATED : HttpStatus.OK).body(resultado.comprobante());
    }

    /** ADMIN: todos. CLIENTE: los suyos. */
    @GetMapping
    public ResponseEntity<List<ComprobanteResponse>> listar() {
        return ResponseEntity.ok(comprobanteService.listar());
    }

    @GetMapping("/reporte")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReporteResponse> reporte(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(comprobanteService.reporte(desde, hasta));
    }

    @GetMapping("/venta/{idVenta}")
    public ResponseEntity<ComprobanteResponse> obtenerPorVenta(@PathVariable Long idVenta) {
        return ResponseEntity.ok(comprobanteService.obtenerPorVenta(idVenta));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComprobanteResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(comprobanteService.obtener(id));
    }

    /** El PDF con el nombre del número: B001-00000040.pdf */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        Comprobante comprobante = comprobanteService.buscarPropio(id);
        String nombre = ComprobanteMapper.numero(comprobante) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(pdfService.generar(comprobante));
    }
}
