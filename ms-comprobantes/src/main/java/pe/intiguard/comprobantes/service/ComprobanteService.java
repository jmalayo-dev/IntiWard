package pe.intiguard.comprobantes.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.intiguard.comprobantes.dto.ComprobanteResponse;
import pe.intiguard.comprobantes.exception.RecursoNoEncontradoException;
import pe.intiguard.comprobantes.mapper.ComprobanteMapper;
import pe.intiguard.comprobantes.repository.ComprobanteRepository;

import java.util.List;

/** PENDIENTE: emitir(EmitirComprobanteRequest) usando SerieCorrelativo, y generar el PDF. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComprobanteService {

    private final ComprobanteRepository comprobanteRepository;

    public List<ComprobanteResponse> listar() {
        return comprobanteRepository.findAll().stream().map(ComprobanteMapper::toResponse).toList();
    }

    public ComprobanteResponse obtener(Long id) {
        return comprobanteRepository.findById(id)
                .map(ComprobanteMapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comprobante " + id + " no existe"));
    }

    /** B001 + 1 -> "B001-00000001" */
    public static String formatearNumero(String serie, long correlativo) {
        return String.format("%s-%08d", serie, correlativo);
    }
}
