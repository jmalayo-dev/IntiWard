package pe.intiguard.security;

/** Lo que trae un token válido. Queda como principal en el SecurityContext. */
public record UsuarioAutenticado(Long id, String correo, String rol) {

    public boolean esAdmin() {
        return "ADMIN".equals(rol);
    }
}
