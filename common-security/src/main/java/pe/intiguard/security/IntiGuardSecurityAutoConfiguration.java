package pe.intiguard.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Se activa sola en cualquier servicio que importe common-security.
 *
 * ESTADO ACTUAL (bases): deja pasar todas las peticiones para que los servicios
 * se puedan probar mientras se construye el login.
 *
 * PENDIENTE (integrante 2, semana 3):
 *  - Agregar un JwtAuthFilter (OncePerRequestFilter) que lea "Authorization: Bearer ..."
 *    con JwtService y cargue el usuario y el rol en el SecurityContext.
 *  - Cambiar permitAll() por authenticated(), dejando públicos /actuator/health y los GET del catálogo.
 */
// Debe registrarse antes que la cadena por defecto de Spring Boot y que la del actuator;
// si no, esas ganan y piden Basic Auth en todos los endpoints.
@AutoConfiguration(before = SecurityAutoConfiguration.class,
        beforeName = "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(JwtProperties.class)
@EnableMethodSecurity
public class IntiGuardSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtService jwtService(JwtProperties properties) {
        return new JwtService(properties);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
