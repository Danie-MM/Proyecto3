

package cr.petmatchcr.acceso.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import cr.petmatchcr.acceso.dto.LoginRequest;
import cr.petmatchcr.acceso.dto.RegistroRequest;
import cr.petmatchcr.acceso.dto.RegistroResponse;
import cr.petmatchcr.acceso.dto.UsuarioDTO;
import cr.petmatchcr.acceso.entities.RolUsuario;
import cr.petmatchcr.acceso.entities.Usuario;
import cr.petmatchcr.acceso.repositories.UsuarioRepository;
import cr.petmatchcr.security.GoogleTokenVerifierService;
import cr.petmatchcr.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Versión 01
 * Autora: Daniela Morales
 * Descripción: Esta clase gestiona el registro, inicio de sesión
 *              y reactivación.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final Pattern PATRON_NOMBRE =
            Pattern.compile("^[\\p{L}]+(?: [\\p{L}]+)*$");

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    public AuthService(UsuarioRepository usuarioRepository,
                       JwtService jwtService,
                       GoogleTokenVerifierService googleTokenVerifierService) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.googleTokenVerifierService = googleTokenVerifierService;
    }

    /**
     * Registra un usuario después de verificar su cuenta de Google.
     *
     * pide los datos necesarios para el registro.
     * devuelve datos del usuario y token JWT.
     */
    @Transactional
    public ResponseEntity<?> registrar(RegistroRequest request) {

        GoogleIdToken.Payload datosGoogle;

        try {
            datosGoogle = googleTokenVerifierService.verificar(request.getIdToken());
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn("Token es inválido: {}", e.getMessage());
            return crearRespuestaError(401, "El token es inválido.");
        } catch (IOException e) {
            log.error("Error al verificar token de Google", e);
            return crearRespuestaError(503, "No se pudo conectar con Google.");
        }

        if (!esCorreoVerificado(datosGoogle)) {
            return crearRespuestaError(401, "El correo no está verificado.");
        }

        String email = datosGoogle.getEmail();
        String nombre = request.getNombreCompleto();

        if (nombre == null || !PATRON_NOMBRE.matcher(nombre.trim()).matches()) {
            return crearRespuestaError(400, "Solo permite letras y espacios intermedios.");
        }

        nombre = nombre.trim();

        LocalDate fechaNacimiento = request.getFechaNacimiento();

        if (fechaNacimiento == null || fechaNacimiento.isAfter(LocalDate.now())) {
            return crearRespuestaError(400, "La fecha ingresada no es válida.");
        }

        RolUsuario rolUsuario;

        try {
            rolUsuario = RolUsuario.valueOf(request.getRol().trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return crearRespuestaError(400, "El rol no es válido.");
        }

        if (rolUsuario != RolUsuario.ADOPTANTE && rolUsuario != RolUsuario.REFUGIO) {
            return crearRespuestaError(400, "El rol  no tiene permisos para autoregistro.");
        }

        Optional<Usuario> usuarioExistente = usuarioRepository.findByEmail(email);

        if (usuarioExistente.isPresent()) {
            Usuario usuarioActual = usuarioExistente.get();

            if (usuarioActual.isActivo()) {
                return crearRespuestaError(409,
                        "Esta cuenta ya se encuentra registrada.");
            }

            return crearRespuestaError(409,
                    "La cuenta está desactivada.");
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombreCompleto(nombre);
        nuevoUsuario.setEmail(email);
        nuevoUsuario.setFechaNacimiento(fechaNacimiento);
        nuevoUsuario.setRol(rolUsuario);
        nuevoUsuario.setActivo(true);

        nuevoUsuario = usuarioRepository.save(nuevoUsuario);

        log.info("Usuario registrado: {} con rol {}", email, rolUsuario);

        return ResponseEntity.ok(crearRespuesta(nuevoUsuario));
    }

    /**
     * Inicia sesión con una cuenta de Google.
     * Verifica que el usuario esté registrado y activo.
     *
     * recibe Token de Google del usuario.
     * Devuelve datos del usuario y token JWT.
     */
    public ResponseEntity<?> iniciarSesion(LoginRequest request) {

        GoogleIdToken.Payload datosGoogle;

        try {
            datosGoogle = googleTokenVerifierService.verificar(request.getIdToken());
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn("Token de Google inválido: {}", e.getMessage());
            return crearRespuestaError(401, "El token inválido.");
        } catch (IOException e) {
            log.error("Error al verificar token.", e);
            return crearRespuestaError(503, "No se pudo conectar con el servicio.");
        }

        if (!esCorreoVerificado(datosGoogle)) {
            return crearRespuestaError(401, "El correo no está verificado.");
        }

        Optional<Usuario> usuarioEncontrado =
                usuarioRepository.findByEmail(datosGoogle.getEmail());

        if (usuarioEncontrado.isEmpty()) {
            return crearRespuestaError(404,
                    "El usuario no existe.");
        }

        Usuario usuario = usuarioEncontrado.get();

        if (!usuario.isActivo()) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "La cuenta esta desactivada.",
                    "nombreCompleto", usuario.getNombreCompleto(),
                    "email", usuario.getEmail()
            ));
        }

        log.info("Inicio de sesión: {}", usuario.getEmail());

        return ResponseEntity.ok(crearRespuesta(usuario));
    }

    /**
     * Reactiva una cuenta desactivada después de verificar
     *
     * Recibe request Token.
     * Devuelve datos del usuario y token JWT.
     */
    @Transactional
    public ResponseEntity<?> reactivar(LoginRequest request) {

        GoogleIdToken.Payload datosGoogle;

        try {
            datosGoogle = googleTokenVerifierService.verificar(request.getIdToken());
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn("Token de Google inválido: {}", e.getMessage());
            return crearRespuestaError(401, "El token es inválido.");
        } catch (IOException e) {
            log.error("Error al verificar token", e);
            return crearRespuestaError(503, "No se pudo conectar con el servicio.");
        }

        if (!esCorreoVerificado(datosGoogle)) {
            return crearRespuestaError(401, "El correo de Google no está verificado.");
        }

        Optional<Usuario> usuarioEncontrado =
                usuarioRepository.findByEmail(datosGoogle.getEmail());

        if (usuarioEncontrado.isEmpty()) {
            return crearRespuestaError(404,
                    "No se encontró ningún usuario con ese correo electrónico.");
        }

        Usuario usuario = usuarioEncontrado.get();

        if (!usuario.isActivo()) {
            usuario.setActivo(true);
            usuario.setFechaDesactivacion(null);
            usuario.setFechaReactivacion(LocalDateTime.now());

            usuario = usuarioRepository.save(usuario);

            log.info("Cuenta reactivada: {}", usuario.getEmail());
        }

        return ResponseEntity.ok(crearRespuesta(usuario));
    }

    private boolean esCorreoVerificado(GoogleIdToken.Payload datosGoogle) {
        return datosGoogle != null
                && datosGoogle.getEmail() != null
                && Boolean.TRUE.equals(datosGoogle.getEmailVerified());
    }

    private RegistroResponse crearRespuesta(Usuario usuario) {

        String tokenJwt = jwtService.generarToken(
                usuario.getEmail(),
                usuario.getRol().name()
        );

        UsuarioDTO datosUsuario = new UsuarioDTO(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getEmail(),
                usuario.getFechaNacimiento(),
                usuario.getRol(),
                usuario.isActivo()
        );

        return new RegistroResponse(tokenJwt, datosUsuario);
    }

    private ResponseEntity<?> crearRespuestaError(int codigoEstado, String mensaje) {
        return ResponseEntity.status(codigoEstado)
                .body(Map.of("error", mensaje));
    }
}
