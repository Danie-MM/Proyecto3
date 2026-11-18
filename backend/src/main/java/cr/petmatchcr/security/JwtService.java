
package cr.petmatchcr.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Versión 01
 * Autora: Daniela Morales
 *
 * Descripción: Clase permite generar y validar tokens JWT
 *
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private final SesionService sesionService;

    public JwtService(SesionService sesionService) {
        this.sesionService = sesionService;
    }

    /**
     * Genera un token JWT con el correo y rol del usuario.
     *
     * recibe email del usuario.
     * recibe rol Rol del usuario.
     * devuelve Token JWT generado.
     */

    public String generarToken(String email, String rol) {
        Date fechaEmision = new Date();
        Date fechaExpiracion = new Date(fechaEmision.getTime() + expiration);

        String idSesion = sesionService.crear(email, fechaExpiracion.toInstant());

        return Jwts.builder()
                .id(idSesion)
                .subject(email)
                .claim("rol", rol)
                .issuedAt(fechaEmision)
                .expiration(fechaExpiracion)
                .signWith(obtenerClaveFirma())
                .compact();
    }


    /**
     * Obtiene email guardado en el token.
     *
     * recibe token JWT.
     *  devuleve email del usuario.
     */
    public String obtenerEmail(String token) {
        return obtenerClaims(token).getSubject();
    }

    /**
     * Verifica que el token sea válido.
     *
     * recibe token Token JWT.
     * devuelve true si el token es válido o flase si es incorrecto.
     */
    public boolean esValido(String token) {
        try {
            Date fechaExpiracion = obtenerClaims(token).getExpiration();
            return fechaExpiracion != null && fechaExpiracion.after(new Date());
        } catch (Exception e) {
            log.warn("Error al validar token JWT: {}", e.getMessage());
            return false;
        }
    }


    /**
     * Verifica que el token sea válido y la sesión siga activa.
     */
    public boolean validarSesion(String token) {
        try {
            if (!esValido(token)) {
                return false;
            }

            Claims datosToken = obtenerClaims(token);

            String idSesion = datosToken.getId();
            String email = datosToken.getSubject();

            if (idSesion == null || email == null) {
                return false;
            }

            return sesionService.validarYActualizar(idSesion, email);

        } catch (Exception e) {
            log.warn("No se pudo validar la sesión: {}", e.getMessage());
            return false;
        }
    }


    /**
     * Revoca la sesión cuando el usuario cierra sesión.
     */
    public void revocar(String token) {
        Claims datosToken = obtenerClaims(token);

        String idSesion = datosToken.getId();
        String email = datosToken.getSubject();

        if (idSesion == null || email == null) {
            throw new IllegalArgumentException("La sesión no es válida.");
        }

        sesionService.revocar(idSesion, email);
    }



    private SecretKey obtenerClaveFirma() {
        byte[] claveBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(claveBytes);
    }

    private Claims obtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(obtenerClaveFirma())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
