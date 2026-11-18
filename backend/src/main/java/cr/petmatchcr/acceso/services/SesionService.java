
package cr.petmatchcr.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Versión 01
 * Autora: Daniela Morales
 *
 * Descripción: Servicio encargado de controlar las sesiones
 * de los usuarios, su tiempo de inactividad y el cierre de sesión.
 */
@Service
public class SesionService {

    private static final long TIEMPO_INACTIVIDAD = 30 * 60;

    private final JdbcTemplate jdbc;

    public SesionService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Guarda una nueva sesión cuando el usuario inicia sesión.
     */
    public String crear(String email, Instant expira) {

        String idSesion = UUID.randomUUID().toString();
        Instant ahora = Instant.now();

        String sql = """
                INSERT INTO sesiones_acceso (id, email, ultimo_acceso, expira)
                VALUES (?, ?, ?, ?)
                """;

        jdbc.update(sql,
                idSesion,
                email,
                Timestamp.from(ahora),
                Timestamp.from(expira));

        return idSesion;
    }

    /**
     * Comprueba que la sesión siga activa y actualiza el último acceso.
     */
    public boolean validarYActualizar(String id, String email) {

        if (id == null || id.isBlank()) {
            return false;
        }

        Instant ahora = Instant.now();
        Instant limiteInactividad = ahora.minusSeconds(TIEMPO_INACTIVIDAD);

        String sql = """
                UPDATE sesiones_acceso
                SET ultimo_acceso = ?
                WHERE id = ?
                  AND email = ?
                  AND revocada = FALSE
                  AND expira > ?
                  AND ultimo_acceso > ?
                """;

        int filasActualizadas = jdbc.update(sql,
                Timestamp.from(ahora),
                id,
                email,
                Timestamp.from(ahora),
                Timestamp.from(limiteInactividad));

        return filasActualizadas == 1;
    }

    /**
     * Desactiva la sesión cuando el usuario cierra sesión.
     */
    public void revocar(String id, String email) {

        String sql = """
                UPDATE sesiones_acceso
                SET revocada = TRUE
                WHERE id = ? AND email = ?
                """;

        jdbc.update(sql, id, email);
    }
}
