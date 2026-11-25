package cr.petmatchcr.acceso.services;

import cr.petmatchcr.mascotas.api.AmbitosMascotas;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * Versión 01
 * Autora: Daniela Morales
 *
 * Descripción: Permite consultar las organizaciones
 * y los permisos del usuario.
 */
@Service
@Profile("!demo-b01")
public class AmbitosMascotasPersistentes implements AmbitosMascotas {

    private final JdbcTemplate jdbc;

    public AmbitosMascotasPersistentes(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Set<UUID> organizacionesAdministrables(UUID usuarioId) {
        String sql = """
                SELECT o.id
                FROM organizaciones o
                JOIN organizacion_miembros m ON m.organizacion_id = o.id
                JOIN usuarios u ON u.id = m.usuario_id
                WHERE u.id_publico = ?
                AND u.activo = TRUE
                AND o.activa = TRUE
                AND m.activo = TRUE
                AND m.puede_administrar = TRUE
                """;

        var organizaciones = jdbc.queryForList(sql, UUID.class, usuarioId);

        return Set.copyOf(organizaciones);
    }

    @Override
    public String nombreOrganizacion(UUID organizacionId) {
        String sql = """
                SELECT nombre
                FROM organizaciones
                WHERE id = ?
                AND activa = TRUE
                """;

        var nombres = jdbc.queryForList(sql, String.class, organizacionId);

        if (nombres.isEmpty()) {
            return "Organización " + organizacionId;
        }

        return nombres.get(0);
    }
}