package pe.intiguard.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pe.intiguard.auth.entity.Rol;
import pe.intiguard.auth.entity.Usuario;
import pe.intiguard.auth.repository.RolRepository;
import pe.intiguard.auth.repository.UsuarioRepository;

/**
 * Crea el administrador al arrancar si todavía no existe. El SQL no lo inserta
 * para que ninguna credencial real quede en el repositorio (ver infra/mysql/01-auth.sql).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminProperties.class)
public class AdminInicializador implements ApplicationRunner {

    private final AdminProperties admin;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(admin.correo()) || !StringUtils.hasText(admin.clave())) {
            log.warn("ADMIN_CORREO o ADMIN_CLAVE vacíos en .env: no se crea el administrador inicial");
            return;
        }
        if (usuarioRepository.existsByCorreo(admin.correo())) {
            return;
        }
        Rol rolAdmin = rolRepository.findByNombre("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Falta el rol ADMIN en db_auth.rol"));
        usuarioRepository.save(Usuario.builder()
                .nombres(admin.nombres())
                .apellidos(admin.apellidos())
                .correo(admin.correo())
                .claveHash(passwordEncoder.encode(admin.clave()))
                .rol(rolAdmin)
                .build());
        log.info("Administrador inicial creado: {}", admin.correo());
    }
}
