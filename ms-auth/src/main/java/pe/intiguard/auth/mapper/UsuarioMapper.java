package pe.intiguard.auth.mapper;

import pe.intiguard.auth.dto.UsuarioResponse;
import pe.intiguard.auth.entity.Usuario;

public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombres(), u.getApellidos(), u.getCorreo(), u.getTelefono(),
                u.getDireccion(), u.getFotoUrl(), u.getRol().getNombre(), u.isActivo(), u.getCreadoEn());
    }
}
