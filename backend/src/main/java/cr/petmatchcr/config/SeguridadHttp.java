package cr.petmatchcr.config;

import cr.petmatchcr.comun.ErrorApi;
import cr.petmatchcr.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
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

    /** El filtro JWT se ejecuta en la cadena de seguridad, no como segundo filtro del contenedor. */
    @Bean
    FilterRegistrationBean<JwtAuthFilter> registroJwt(JwtAuthFilter filtro) {
        var registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    SecurityFilterChain rutas(
            HttpSecurity http,
            Environment env,
            ObjectMapper objectMapper,
            JwtAuthFilter jwtAuthFilter) throws Exception {

        boolean esEntornoPruebas = env.acceptsProfiles(Profiles.of("local", "test"));
        boolean esDemoB01 = env.acceptsProfiles(Profiles.of("local & demo-b01"));

        PathPatternRequestMatcher.Builder matcherBuilder = PathPatternRequestMatcher.withDefaults();
        var pruebaPost = matcherBuilder.matcher(HttpMethod.POST, "/api/v1/pruebas-entorno");
        var pruebaGet = matcherBuilder.matcher(HttpMethod.GET, "/api/v1/pruebas-entorno/{id}");

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(Customizer.withDefaults())
                // IF_REQUIRED es el valor por defecto. La estrategia debe ser explícita:
                // un JWT no inicia una sesión ni invalida su token CSRF en cada petición.
                .sessionManagement(session -> session.requireExplicitAuthenticationStrategy(true))
                .securityContext(context -> context.requireExplicitSave(true))
                .authorizeHttpRequests(auth -> {
                    auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();

                    auth.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/sesion/csrf").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/catalogos/mascotas").permitAll();
                    auth.requestMatchers(HttpMethod.POST,
                            "/api/auth/registro", "/api/auth/login", "/api/auth/reactivar").permitAll();

                    if (esDemoB01) {
                        auth.requestMatchers(HttpMethod.GET, "/api/v1/demo-b01/actores").permitAll();
                        auth.requestMatchers(HttpMethod.POST, "/api/v1/demo-b01/sesion").permitAll();
                    }

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
                "http://localhost:5173",
                "http://127.0.0.1:4200",
                "http://127.0.0.1:5173"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-CSRF-TOKEN", "Idempotency-Key"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Location"));

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
