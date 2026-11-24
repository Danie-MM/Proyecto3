
package cr.petmatchcr.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Versión 01
 * Autora: Daniela Morales
 *
 * Descripción: Servicio encargado de controlar los intentos
 * fallidos de inicio de sesión y bloquear temporalmente el acceso.
 */
@Service
public class IntentosAccesoService {

    private static final int MAX_INTENTOS = 5;
    private static final long TIEMPO_BLOQUEO = 15 * 60;

    private final JdbcTemplate jdbc;

    public IntentosAccesoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Comprueba si una dirección IP tiene el acceso bloqueado.
     */
    public boolean bloqueado(String origen) {

        String sql = """
                SELECT bloqueado_hasta
                FROM intentos_acceso
                WHERE origen = ?
                """;

        return Boolean.TRUE.equals(jdbc.query(sql, rs -> {
            if (rs.next()) {
                Timestamp fechaBloqueo = rs.getTimestamp("bloqueado_hasta");

                return fechaBloqueo != null
                        && fechaBloqueo.toInstant().isAfter(Instant.now());
            }

            return false;
        }, origen));
    }

    /**
     * Registra un intento fallido y bloquea el acceso
     * cuando se llega a cinco intentos en quince minutos.
     */
    public void fallo(String origen) {

        Instant ahora = Instant.now();
        Instant inicioVentana = ahora.minusSeconds(TIEMPO_BLOQUEO);
        Instant finBloqueo = ahora.plusSeconds(TIEMPO_BLOQUEO);

        String sql = """
                INSERT INTO intentos_acceso
                    (origen, intentos, inicio_ventana, bloqueado_hasta)
                VALUES (?, 1, ?, NULL)
                ON CONFLICT (origen) DO UPDATE SET
                    intentos = CASE
                        WHEN intentos_acceso.inicio_ventana <= ? THEN 1
                        ELSE intentos_acceso.intentos + 1
                    END,
                    inicio_ventana = CASE
                        WHEN intentos_acceso.inicio_ventana <= ? THEN ?
                        ELSE intentos_acceso.inicio_ventana
                    END,
                    bloqueado_hasta = CASE
                        WHEN intentos_acceso.inicio_ventana > ?
                             AND intentos_acceso.intentos + 1 >= ?
                        THEN CAST(? AS TIMESTAMPTZ)
                        ELSE NULL
                    END
                """;

        jdbc.update(sql,
                origen,
                Timestamp.from(ahora),
                Timestamp.from(inicioVentana),
                Timestamp.from(inicioVentana),
                Timestamp.from(ahora),
                Timestamp.from(inicioVentana),
                MAX_INTENTOS,
                Timestamp.from(finBloqueo));
    }

    /**
     * Elimina los intentos fallidos después de iniciar sesión.
     */
    public void exito(String origen) {

        String sql = "DELETE FROM intentos_acceso WHERE origen = ?";

        jdbc.update(sql, origen);
    }
}
