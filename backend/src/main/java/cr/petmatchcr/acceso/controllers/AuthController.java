package cr.petmatchcr.acceso.controllers;

import cr.petmatchcr.acceso.dto.LoginRequest;
import cr.petmatchcr.acceso.dto.RegistroRequest;
import cr.petmatchcr.acceso.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import cr.petmatchcr.security.JwtService;
import cr.petmatchcr.security.IntentosAccesoService;
import jakarta.servlet.http.HttpServletRequest;
/**
 * Versión 01
 * Autora: Daniela Morales
 *
 * Descripción: Controlador REST que expone los endpoints
 *  gestión de autenticación (registro, login y reactivaciones).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final IntentosAccesoService intentos;

    //  servicio de autenticación por constructor
    public AuthController(AuthService authService, JwtService jwtService,
                          IntentosAccesoService intentos) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.intentos = intentos;
    }
    /**
     *
     * validando previamente su identidad con el token de Google.
     */
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@Valid @RequestBody RegistroRequest datosRegistro) {
        return authService.registrar(datosRegistro);
    }

    /**
     * Endpoint para el inicio de sesión.
     */
    @PostMapping("/login")
    public ResponseEntity<?> iniciarSesion(@Valid @RequestBody LoginRequest datosLogin,
                                           HttpServletRequest request) {
        String origen = request.getRemoteAddr();

        if (intentos.bloqueado(origen)) {
            return ResponseEntity.status(429)
                    .body(java.util.Map.of("error", "Demasiados intentos. Intente más tarde."));
        }

        ResponseEntity<?> resultado = authService.iniciarSesion(datosLogin);

        if (resultado.getStatusCode().is2xxSuccessful()) {
            intentos.exito(origen);
        } else {
            int estado = resultado.getStatusCode().value();

            if (estado == 401 || estado == 403 || estado == 404) {
                intentos.fallo(origen);
            }
        }

        return resultado;
    }

    /**
     * Endpoint para habilitar nuevamente una cuenta.
     */
    @PostMapping("/reactivar")
    public ResponseEntity<?> reactivar(@Valid @RequestBody LoginRequest datosReactivacion) {
        return authService.reactivar(datosReactivacion);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> cerrarSesion(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        try {
            jwtService.revocar(header.substring(7));
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }
}
