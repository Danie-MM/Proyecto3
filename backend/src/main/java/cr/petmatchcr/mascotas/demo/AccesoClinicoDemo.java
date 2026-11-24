package cr.petmatchcr.mascotas.demo;

import cr.petmatchcr.mascotas.clinica.AccesoClinico;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Padrón y auditoría de ensayo del esquema demo. La implementación integrada corresponde a A-03. */
@Component
@Profile("local & demo-b01 & !integracion")
public class AccesoClinicoDemo implements AccesoClinico {
    static final UUID VETERINARIO = UUID.fromString("55555555-5555-4555-8555-555555555555");
    private final JdbcTemplate jdbc;

    AccesoClinicoDemo(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public boolean esVeterinarioHabilitado(UUID usuarioId) {
        return VETERINARIO.equals(usuarioId) && Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM usuarios WHERE id_publico = ? AND activo)", Boolean.class, usuarioId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void auditarPermiso(Accion accion, UUID actorId, UUID permisoId, UUID mascotaId) {
        jdbc.update("INSERT INTO auditoria_permiso_clinico_demo(accion,actor_id,permiso_id,mascota_id) VALUES (?,?,?,?)",
                accion.name(), actorId, permisoId, mascotaId);
    }
}
