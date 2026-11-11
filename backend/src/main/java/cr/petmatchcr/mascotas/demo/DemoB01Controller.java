package cr.petmatchcr.mascotas.demo;

import cr.petmatchcr.mascotas.service.ErrorMascota;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Permite ensayar B sin sustituir el trabajo de A. Solo existe con local + demo-b01.
 * El navegador elige entre cuatro actores ficticios conocidos; no envía un dueño en el perfil.
 * Esta sesión de demostración NO acredita integración con registro/ingreso/roles reales.
 */
@RestController
@Profile("local & demo-b01")
@RequestMapping("/api/v1/demo-b01")
public class DemoB01Controller {
    private final ActoresDemoB01 actores;
    private final HttpSessionSecurityContextRepository sesiones = new HttpSessionSecurityContextRepository();

    public DemoB01Controller(ActoresDemoB01 actores) { this.actores = actores; }

    @GetMapping("/actores")
    public ResponseEntity<ActoresRespuesta> listar() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        UUID actual = null;
        if (autenticacion != null) {
            try { actual = UUID.fromString(autenticacion.getName()); }
            catch (IllegalArgumentException ignored) { }
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new ActoresRespuesta(actores.listar(), actual));
    }

    @PostMapping(value = "/sesion", consumes = "application/json")
    public ResponseEntity<SesionDemoSolicitud> seleccionar(@Valid @RequestBody SesionDemoSolicitud solicitud,
            HttpServletRequest request, HttpServletResponse response) {
        if (!actores.existe(solicitud.actorId())) throw new ErrorMascota(400, "VALIDACION", "Elegí un actor de demostración.");
        request.getSession(true);
        request.changeSessionId();
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(solicitud.actorId().toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USUARIO"))));
        SecurityContextHolder.setContext(contexto);
        sesiones.saveContext(contexto, request, response);
        // Cambiar de actor invalida el token anterior. Angular debe pedir uno nuevo.
        new HttpSessionCsrfTokenRepository().saveToken(null, request, response);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(solicitud);
    }

    public record ActoresRespuesta(List<ActoresDemoB01.ActorDemo> actores, UUID actorActualId) {}
    public record SesionDemoSolicitud(@NotNull UUID actorId) {}
}
