package pe.intiguard.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.intiguard.auth.dto.RegistroRequest;
import pe.intiguard.auth.dto.UsuarioResponse;
import pe.intiguard.auth.entity.Rol;
import pe.intiguard.auth.entity.Usuario;
import pe.intiguard.auth.exception.CorreoDuplicadoException;
import pe.intiguard.auth.exception.DatosInvalidosException;
import pe.intiguard.auth.exception.RecursoNoEncontradoException;
import pe.intiguard.auth.mapper.UsuarioMapper;
import pe.intiguard.auth.repository.RolRepository;
import pe.intiguard.auth.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Cuentas de usuario: registro público, perfil propio y administración (solo ADMIN).
 * Nunca se borra una cuenta: DELETE la desactiva, porque puede tener ventas.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    static final String ROL_ADMIN = "ADMIN";
    static final String ROL_CLIENTE = "CLIENTE";
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAllByOrderByIdAsc().stream().map(UsuarioMapper::toResponse).toList();
    }

    public UsuarioResponse obtener(Long id) {
        return UsuarioMapper.toResponse(buscar(id));
    }

    /** Registro público: siempre CLIENTE, aunque el cuerpo diga otra cosa. */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        return crear(request, ROL_CLIENTE);
    }

    /** Alta desde el panel: ADMIN o CLIENTE; cualquier otro valor se toma como CLIENTE. */
    @Transactional
    public UsuarioResponse crearDesdePanel(RegistroRequest request) {
        String rol = ROL_ADMIN.equalsIgnoreCase(textoONulo(request.rol())) ? ROL_ADMIN : ROL_CLIENTE;
        return crear(request, rol);
    }

    /** El usuario edita su perfil. correo, rol y activo no se cambian por aquí: se ignoran. */
    @Transactional
    public UsuarioResponse actualizarPerfil(Long idUsuario, Map<String, Object> cambios) {
        Usuario usuario = buscar(idUsuario);
        List<String> detalles = new ArrayList<>();
        aplicarDatosPersonales(usuario, cambios, detalles);
        aplicarClave(usuario, cambios, detalles);
        lanzarSiHayErrores(detalles);
        return UsuarioMapper.toResponse(usuario);
    }

    /** El ADMIN edita cualquier cuenta. { "activo": true } también levanta el bloqueo por intentos. */
    @Transactional
    public UsuarioResponse actualizar(Long id, Map<String, Object> cambios) {
        Usuario usuario = buscar(id);
        List<String> detalles = new ArrayList<>();
        aplicarDatosPersonales(usuario, cambios, detalles);
        aplicarClave(usuario, cambios, detalles);

        if (cambios.containsKey("correo")) {
            String correo = textoONulo(cambios.get("correo"));
            if (correo == null || !CORREO.matcher(correo).matches() || correo.length() > 120) {
                detalles.add("correo: no tiene formato de correo");
            } else if (!correo.equalsIgnoreCase(usuario.getCorreo())) {
                if (usuarioRepository.existsByCorreo(correo)) {
                    lanzarSiHayErrores(detalles);
                    throw new CorreoDuplicadoException();
                }
                usuario.setCorreo(correo);
            }
        }
        if (cambios.containsKey("rol")) {
            String rol = textoONulo(cambios.get("rol"));
            if (ROL_ADMIN.equalsIgnoreCase(rol) || ROL_CLIENTE.equalsIgnoreCase(rol)) {
                usuario.setRol(rol(rol.toUpperCase()));
            } else {
                detalles.add("rol: debe ser ADMIN o CLIENTE");
            }
        }
        if (cambios.containsKey("activo")) {
            if (cambios.get("activo") instanceof Boolean activo) {
                usuario.setActivo(activo);
                if (activo) {
                    usuario.levantarBloqueo();
                }
            } else {
                detalles.add("activo: debe ser true o false");
            }
        }
        lanzarSiHayErrores(detalles);
        return UsuarioMapper.toResponse(usuario);
    }

    @Transactional
    public void desactivar(Long id) {
        buscar(id).setActivo(false);
    }

    private UsuarioResponse crear(RegistroRequest request, String nombreRol) {
        String correo = request.correo().trim();
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new CorreoDuplicadoException();
        }
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombres(request.nombres().trim())
                .apellidos(request.apellidos().trim())
                .correo(correo)
                .telefono(textoONulo(request.telefono()))
                .direccion(textoONulo(request.direccion()))
                .claveHash(passwordEncoder.encode(request.password()))
                .rol(rol(nombreRol))
                .build());
        return UsuarioMapper.toResponse(usuario);
    }

    private void aplicarDatosPersonales(Usuario usuario, Map<String, Object> cambios, List<String> detalles) {
        if (cambios.containsKey("nombres")) {
            String nombres = obligatorio(cambios, "nombres", 80, detalles);
            if (nombres != null) usuario.setNombres(nombres);
        }
        if (cambios.containsKey("apellidos")) {
            String apellidos = obligatorio(cambios, "apellidos", 80, detalles);
            if (apellidos != null) usuario.setApellidos(apellidos);
        }
        if (cambios.containsKey("telefono") && cabe(cambios, "telefono", 15, detalles)) {
            usuario.setTelefono(textoONulo(cambios.get("telefono")));
        }
        if (cambios.containsKey("direccion") && cabe(cambios, "direccion", 200, detalles)) {
            usuario.setDireccion(textoONulo(cambios.get("direccion")));
        }
        // fotoUrl: null quita la foto
        if (cambios.containsKey("fotoUrl") && cabe(cambios, "fotoUrl", 255, detalles)) {
            usuario.setFotoUrl(textoONulo(cambios.get("fotoUrl")));
        }
    }

    private void aplicarClave(Usuario usuario, Map<String, Object> cambios, List<String> detalles) {
        Object valor = cambios.get("password");
        if (valor == null) {
            return;
        }
        String clave = valor.toString();
        if (clave.length() < 8 || clave.length() > 72) {
            detalles.add("password: debe tener entre 8 y 72 caracteres");
            return;
        }
        usuario.setClaveHash(passwordEncoder.encode(clave));
    }

    private String obligatorio(Map<String, Object> cambios, String campo, int max, List<String> detalles) {
        String valor = textoONulo(cambios.get(campo));
        if (valor == null) {
            detalles.add(campo + ": es obligatorio");
            return null;
        }
        if (valor.length() > max) {
            detalles.add(campo + ": admite hasta " + max + " caracteres");
            return null;
        }
        return valor;
    }

    private boolean cabe(Map<String, Object> cambios, String campo, int max, List<String> detalles) {
        String valor = textoONulo(cambios.get(campo));
        if (valor != null && valor.length() > max) {
            detalles.add(campo + ": admite hasta " + max + " caracteres");
            return false;
        }
        return true;
    }

    private static void lanzarSiHayErrores(List<String> detalles) {
        if (!detalles.isEmpty()) {
            throw new DatosInvalidosException(detalles);
        }
    }

    /** Texto recortado, o null si no vino o vino vacío. */
    private static String textoONulo(Object valor) {
        if (valor == null) return null;
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + id + " no existe"));
    }

    private Rol rol(String nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + nombre + " en db_auth.rol"));
    }
}
