package cr.petmatchcr.extravios.repository;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Serializa reenvíos de una misma intención dentro de la transacción del servicio. */
@Repository
@RequiredArgsConstructor
public class IntentosUbicacionRepository {
    private final JdbcTemplate jdbc;

    public record Intento(String huella, UUID preferenciaId, String respuestaJson) {}

    /** INSERT espera si otro envío tiene la misma clave; FOR UPDATE protege la lectura posterior. */
    public Intento reservar(UUID usuarioId, UUID clave, String huella) {
        jdbc.update("""
                INSERT INTO ubicacion_intento_creacion (usuario_id, clave, huella)
                VALUES (?, ?, ?) ON CONFLICT (usuario_id, clave) DO NOTHING
                """, usuarioId, clave, huella);
        return jdbc.queryForObject("""
                SELECT huella, preferencia_id, respuesta_json FROM ubicacion_intento_creacion
                WHERE usuario_id = ? AND clave = ? FOR UPDATE
                """, (fila, numero) -> new Intento(fila.getString("huella"),
                fila.getObject("preferencia_id", UUID.class), fila.getString("respuesta_json")), usuarioId, clave);
    }

    /** La respuesta y la preferencia se confirman juntas o se revierten juntas. */
    public void completar(UUID usuarioId, UUID clave, UUID preferenciaId, String respuestaJson) {
        jdbc.update("""
                UPDATE ubicacion_intento_creacion SET preferencia_id = ?, respuesta_json = ?
                WHERE usuario_id = ? AND clave = ?
                """, preferenciaId, respuestaJson, usuarioId, clave);
    }
}
