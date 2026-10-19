package pe.intiguard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Se activa sola en cualquier servicio que importe common-security (contrato C2).
 * Cada servicio declara sus rutas públicas en intiguard.security.rutas-publicas;
 * con intiguard.security.exigir-token=true, el resto exige un JWT válido.
 */
// Debe registrarse antes que la cadena por defecto de Spring Boot y que la del actuator;
// si no, esas ganan y piden Basic Auth en todos los endpoints.
@AutoConfiguration(before = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class},
        beforeName = "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties({JwtProperties.class, SeguridadProperties.class})
@EnableMethodSecurity
public class IntiGuardSecurityAutoConfiguration {

    private static final String[] SIEMPRE_PUBLICAS = {"/actuator/health/**", "/error"};

    @Bean
    @ConditionalOnMissingBean
    public JwtService jwtService(JwtProperties properties) {
        return new JwtService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public UsuarioActual usuarioActual() {
        return new UsuarioActual();
    }

    @Bean
    @ConditionalOnMissingBean
    public RespuestaErrorJson respuestaErrorJson(ObjectMapper objectMapper) {
        return new RespuestaErrorJson(objectMapper);
    }

    /** El login lo hace ms-auth con su PasswordEncoder; este bean solo evita el usuario y la clave que Spring genera por defecto. */
    @Bean
    @ConditionalOnMissingBean
    public AuthenticationManager authenticationManager() {
        return autenticacion -> {
            throw new ProviderNotFoundException("La autenticación se hace con JWT");
        };
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService, SeguridadProperties seguridad,
                                                   RespuestaErrorJson respuestaError) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .exceptionHandling(e -> e.authenticationEntryPoint(respuestaError).accessDeniedHandler(respuestaError))
            .addFilterBefore(new JwtAuthFilter(jwtService, respuestaError), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new ClaveInternaFilter(seguridad, respuestaError), JwtAuthFilter.class)
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(SIEMPRE_PUBLICAS).permitAll();
                for (String regla : seguridad.getRutasPublicas()) {
                    String[] partes = regla.trim().split("\\s+", 2);
                    if (partes.length == 2) {
                        auth.requestMatchers(HttpMethod.valueOf(partes[0].toUpperCase()), partes[1]).permitAll();
                    } else {
                        auth.requestMatchers(partes[0]).permitAll();
                    }
                }
                if (seguridad.isExigirToken()) {
                    auth.anyRequest().authenticated();
                } else {
                    auth.anyRequest().permitAll();
                }
            });
        return http.build();
    }
}
