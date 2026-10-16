package pe.intiguard.inventario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.intiguard.inventario.dto.StockResponse;
import pe.intiguard.inventario.exception.RecursoNoEncontradoException;
import pe.intiguard.inventario.mapper.StockMapper;
import pe.intiguard.inventario.repository.StockRepository;

/** PENDIENTE: reservar, confirmar y liberar, cada uno registrando su MovimientoStock. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;

    public StockResponse obtener(Long idProducto) {
        return stockRepository.findById(idProducto)
                .map(StockMapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay stock registrado para el producto " + idProducto));
    }
}
