package pe.intiguard.auth.mapper;

import pe.intiguard.auth.dto.UsuarioResponse;
import pe.intiguard.auth.entity.Usuario;

import java.time.LocalDateTime;

public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static UsuarioResponse toResponse(Usuario u) {
        LocalDateTime bloqueo = u.getBloqueadoHasta();
        // Un bloqueo vencido no se muestra: la cuenta ya puede entrar
        LocalDateTime bloqueoVigente = bloqueo != null && bloqueo.isAfter(LocalDateTime.now()) ? bloqueo : null;
        return new UsuarioResponse(u.getId(), u.getNombres(), u.getApellidos(), u.getCorreo(), u.getTelefono(),
                u.getDireccion(), u.getFotoUrl(), u.getRol().getNombre(), u.isActivo(), u.getCreadoEn(),
                bloqueoVigente, u.getUltimoAcceso());
    }
}
