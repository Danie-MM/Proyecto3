package cr.petmatchcr.config;

import cr.petmatchcr.comun.ErrorApi;
import cr.petmatchcr.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * Configuración de seguridad HTTP y control de accesos de la aplicación.
 *
 * autora: Daniela Morales
 */
@Configuration
@EnableMethodSecurity
class SeguridadHttp {

    @Bean
    SecurityFilterChain rutas(
            HttpSecurity http,
            Environment env,
            ObjectMapper objectMapper,
            JwtAuthFilter jwtAuthFilter) throws Exception {

        boolean esEntornoPruebas = env.acceptsProfiles(Profiles.of("local", "test"));

        PathPatternRequestMatcher.Builder matcherBuilder = PathPatternRequestMatcher.withDefaults();
        var pruebaPost = matcherBuilder.matcher(HttpMethod.POST, "/api/v1/pruebas-entorno");
        var pruebaGet = matcherBuilder.matcher(HttpMethod.GET, "/api/v1/pruebas-entorno/{id}");

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();

                    auth.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll();
                    auth.requestMatchers(
                            "/api/auth/registro", "/api/auth/login", "/api/auth/reactivar").permitAll();

                    if (esEntornoPruebas) {
                        auth.requestMatchers(pruebaPost, pruebaGet).permitAll();
                    }

                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((req, res, ex) ->
                                escribirErrorJson(res, objectMapper, 401,
                                        "AUTENTICACION_REQUERIDA",
                                        "Se necesita inicio de sesión para acceder."))
                        .accessDeniedHandler((req, res, ex) ->
                                escribirErrorJson(res, objectMapper, 403,
                                        "ACCESO_DENEGADO",
                                        "No cuenta con los permisos necesarios.")))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://localhost:5173"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void escribirErrorJson(
            HttpServletResponse response,
            ObjectMapper mapper,
            int status,
            String codigo,
            String mensaje) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        response.getWriter().write(
                mapper.writeValueAsString(ErrorApi.crear(codigo, mensaje))
        );
    }
}
