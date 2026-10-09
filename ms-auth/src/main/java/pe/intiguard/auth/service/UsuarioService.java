package pe.intiguard.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.intiguard.auth.dto.UsuarioResponse;
import pe.intiguard.auth.exception.RecursoNoEncontradoException;
import pe.intiguard.auth.mapper.UsuarioMapper;
import pe.intiguard.auth.repository.UsuarioRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioMapper::toResponse).toList();
    }

    public UsuarioResponse obtener(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioMapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + id + " no existe"));
    }
}
