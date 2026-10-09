package pe.intiguard.catalogo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.intiguard.catalogo.dto.ProductoResponse;
import pe.intiguard.catalogo.exception.RecursoNoEncontradoException;
import pe.intiguard.catalogo.mapper.ProductoMapper;
import pe.intiguard.catalogo.repository.ProductoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;

    public List<ProductoResponse> listar() {
        return productoRepository.findByActivoTrue().stream().map(ProductoMapper::toResponse).toList();
    }

    public ProductoResponse obtener(Long id) {
        return productoRepository.findById(id)
                .map(ProductoMapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + id + " no existe"));
    }
}
