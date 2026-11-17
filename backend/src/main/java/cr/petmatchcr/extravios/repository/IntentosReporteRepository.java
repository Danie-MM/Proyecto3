
package cr.petmatchcr.extravios.repository;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Controla los reintentos y la publicación concurrente
 * de reportes de extravío.
 */
@Repository
@RequiredArgsConstructor
public class IntentosReporteRepository {

    private final JdbcTemplate jdbc;

    public record Intento(
            String huella,
            UUID reporteId,
            String respuestaJson,
            Integer estadoHttp
    ) {}

    /**
     * Reserva una intención de publicación.
     */
    public Intento reservar(
            UUID usuarioId,
            UUID clave,
            String huella
    ) {
        jdbc.update("""
                INSERT INTO reporte_extravio_intento_creacion
                    (usuario_id, clave, huella)
                VALUES (?, ?, ?)
                ON CONFLICT (usuario_id, clave) DO NOTHING
                """, usuarioId, clave, huella);

        return jdbc.queryForObject("""
                SELECT huella, reporte_id,
                       respuesta_json, estado_http
                FROM reporte_extravio_intento_creacion
                WHERE usuario_id = ? AND clave = ?
                FOR UPDATE
                """,
                (fila, numero) -> new Intento(
                        fila.getString("huella"),
                        fila.getObject("reporte_id", UUID.class),
                        fila.getString("respuesta_json"),
                        fila.getObject("estado_http", Integer.class)
                ),
                usuarioId,
                clave
        );
    }

    /**
     * Serializa las publicaciones concurrentes
     * correspondientes a una misma mascota.
     */
    public void bloquearMascota(UUID mascotaId) {
        jdbc.query("""
                SELECT pg_advisory_xact_lock(
                    hashtextextended(?, 0)
                )
                """,
                fila -> {
                    // El bloqueo se libera al terminar
                    // la transacción.
                },
                "extravios:" + mascotaId
        );
    }

    /**
     * Conserva el resultado original de la publicación.
     */
    public void completar(
            UUID usuarioId,
            UUID clave,
            UUID reporteId,
            String respuestaJson,
            int estadoHttp
    ) {
        jdbc.update("""
                UPDATE reporte_extravio_intento_creacion
                SET reporte_id = ?,
                    respuesta_json = ?,
                    estado_http = ?
                WHERE usuario_id = ? AND clave = ?
                """,
                reporteId,
                respuestaJson,
                estadoHttp,
                usuarioId,
                clave
        );
    }
}
