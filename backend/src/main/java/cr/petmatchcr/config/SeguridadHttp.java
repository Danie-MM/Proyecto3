package cr.petmatchcr.config;

import cr.petmatchcr.comun.ErrorApi;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
class SeguridadHttp {

    // NOTA: Si IntelliJ te marca un error en "http.build()", agrégale "throws Exception" al lado del nombre de este método
    @Bean
    SecurityFilterChain rutas(HttpSecurity http, Environment entorno, ObjectMapper json) throws Exception {
        boolean pruebaTecnica = entorno.acceptsProfiles(Profiles.of("local", "test"));
        var rutas = PathPatternRequestMatcher.withDefaults();
        var guardar = rutas.matcher(HttpMethod.POST, "/api/v1/pruebas-entorno");
        var consultar = rutas.matcher(HttpMethod.GET, "/api/v1/pruebas-entorno/{id}");

        // --- 1. Cree la ruta de tus campañas ---
        var campanias = rutas.matcher("/api/v1/campanias/**");

        http.authorizeHttpRequests(reglas -> {
            reglas.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll();

            // --- 2. Damos permiso libre a tu módulo ---
            reglas.requestMatchers(campanias).permitAll();

            if (pruebaTecnica) {
                reglas.requestMatchers(guardar, consultar).permitAll();
            }
            reglas.anyRequest().denyAll();
        });

        // --- 3. Apagamos el bloqueo CSRF solo para tus campañas (necesario para Postman) ---
        http.csrf(csrf -> {
            if (pruebaTecnica) {
                csrf.ignoringRequestMatchers(guardar, campanias);
            } else {
                csrf.ignoringRequestMatchers(campanias);
            }
        });

        http.formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((req, res, ex) -> responder(res, json, 401,
                                "AUTENTICACION_REQUERIDA", "Esta ruta requiere autenticación."))
                        .accessDeniedHandler((req, res, ex) -> responder(res, json, 403,
                                "ACCESO_DENEGADO", "No tenés permiso para realizar esta operación.")));
        return http.build();
    }

    private void responder(HttpServletResponse respuesta, ObjectMapper json, int estado,
                           String codigo, String mensaje) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write(json.writeValueAsString(ErrorApi.crear(codigo, mensaje)));
    }
}