package cr.petmatchcr.security;

import cr.petmatchcr.comun.ErrorApi;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Valida cada Bearer contra una cuenta activa, sin reutilizar una sesión si el token falla. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService,
                         ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            // El perfil demo-b01 usa su sesión explícita; las llamadas reales envían JWT.
            filterChain.doFilter(request, response);
            return;
        }

        // Un Bearer presente siempre prevalece sobre la autenticación de una sesión anterior.
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        if (!authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            rechazar(response);
            return;
        }
        String token = authHeader.substring(7).trim();
        if (token.isEmpty() || !jwtService.esValido(token)) {
            rechazar(response);
            return;
        }

        try {
            String email = jwtService.obtenerEmail(token);
            if (email == null || email.isBlank()) {
                rechazar(response);
                return;
            }
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()
                    || !userDetails.isAccountNonExpired() || !userDetails.isCredentialsNonExpired()) {
                rechazar(response);
                return;
            }
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            rechazar(response);
            return;
        }

        // No se guarda el JWT como autenticación permanente de la sesión de CSRF.
        filterChain.doFilter(request, response);
    }

    private void rechazar(HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ErrorApi.crear(
                "AUTENTICACION_REQUERIDA", "La sesión no es válida. Iniciá sesión de nuevo.")));
    }
}
