package cr.petmatchcr.acceso.controllers;

import cr.petmatchcr.acceso.dto.LoginRequest;
import cr.petmatchcr.acceso.dto.RegistroRequest;
import cr.petmatchcr.acceso.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    //  servicio de autenticación por constructor
    public AuthController(AuthService authService) {
        this.authService = authService;
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
    public ResponseEntity<?> iniciarSesion(@Valid @RequestBody LoginRequest datosLogin) {
        return authService.iniciarSesion(datosLogin);
    }

    /**
     * Endpoint para habilitar nuevamente una cuenta.
     */
    @PostMapping("/reactivar")
    public ResponseEntity<?> reactivar(@Valid @RequestBody LoginRequest datosReactivacion) {
        return authService.reactivar(datosReactivacion);
    }
}
