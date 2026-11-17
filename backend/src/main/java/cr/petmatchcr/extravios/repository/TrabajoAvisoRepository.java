
package cr.petmatchcr.extravios.repository;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Gestiona la persistencia de los trabajos de avisos
 * asociados a los reportes de extravío.
 */
@Repository
@RequiredArgsConstructor
public class TrabajoAvisoRepository {

    private final JdbcTemplate jdbc;

    /**
     * Registra un trabajo pendiente de avisos por proximidad.
     * Cada reporte puede tener un único trabajo asociado.
     *
     * @param reporteId identificador del reporte de extravío
     */
    public void registrarPendiente(UUID reporteId) {

        jdbc.update("""
                INSERT INTO reporte_extravio_trabajo_aviso (
                    reporte_id,
                    estado
                )
                VALUES (?, 'PENDIENTE')
                ON CONFLICT (reporte_id) DO NOTHING
                """,
                reporteId
        );
    }
}
