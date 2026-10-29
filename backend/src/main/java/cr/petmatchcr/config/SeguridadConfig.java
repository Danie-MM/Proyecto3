package cr.petmatchcr.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import cr.petmatchcr.comun.ErrorRespuesta;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Abre solo la comprobación local; Daniela implementará el acceso del bloque A. */
@Configuration
public class SeguridadConfig {

    /** Permite salud y las dos rutas técnicas; conserva CSRF fuera de la prueba. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment,
                                                    ObjectMapper objectMapper) throws Exception {
        boolean entornoTecnico = environment.acceptsProfiles(Profiles.of("local", "test"));
        var guardar = new AntPathRequestMatcher("/api/v1/pruebas-entorno", "POST");
        var consultar = new AntPathRequestMatcher("/api/v1/pruebas-entorno/*", "GET");
        var salud = new AntPathRequestMatcher("/actuator/health", "GET");

        http.authorizeHttpRequests(reglas -> {
            reglas.requestMatchers(salud).permitAll();
            if (entornoTecnico) {
                reglas.requestMatchers(guardar, consultar).permitAll();
            }
            reglas.anyRequest().denyAll();
        });
        if (entornoTecnico) {
            http.csrf(csrf -> csrf.ignoringRequestMatchers(guardar));
        }
        http.httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((request, response, exception) -> escribirError(
                                response, objectMapper, 401, "AUTENTICACION_REQUERIDA",
                                "Esta ruta no está habilitada para acceso anónimo."))
                        .accessDeniedHandler((request, response, exception) -> escribirError(
                                response, objectMapper, 403, "ACCESO_DENEGADO",
                                "No tenés permiso para realizar esta operación.")));
        return http.build();
    }

    private void escribirError(HttpServletResponse response, ObjectMapper mapper, int status,
                               String codigo, String mensaje) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ErrorRespuesta.crear(codigo, mensaje, List.of()));
    }
}
