package cr.petmatchcr.mascotas.repository;

import cr.petmatchcr.mascotas.dto.CrearMascotaSolicitud;
import cr.petmatchcr.mascotas.dto.EditarMascotaSolicitud;
import cr.petmatchcr.mascotas.dto.DesactivarMascotaSolicitud;
import cr.petmatchcr.mascotas.model.Comportamiento;
import cr.petmatchcr.mascotas.model.Especie;
import cr.petmatchcr.mascotas.model.Mascota;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.SqlArrayValue;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MascotaRepository {

    private final JdbcTemplate jdbc;

    public MascotaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID crearIndividual(UUID responsableId, CrearMascotaSolicitud solicitud) {
        var etiquetas = new SqlArrayValue("uuid", solicitud.comportamientoIds().toArray());

        return jdbc.queryForObject(
                "CALL create_mascota_pr(?, ?, ?, ?, ?, ?, ?, NULL)",
                UUID.class,
                responsableId,
                solicitud.nombre(),
                solicitud.especie(),
                solicitud.edadEstimadaMeses(),
                solicitud.tamano(),
                solicitud.energia(),
                etiquetas
        );
    }

    /**
     * El servicio entrega el actor y los ámbitos ya verificados por A. La base vuelve
     * a comprobarlos y guarda el perfil con una clave que permite reintentos seguros.
     */
    @Transactional
    public Mascota crear(UUID actorId, Set<UUID> organizaciones, UUID clave, CrearMascotaSolicitud solicitud) {
        UUID id = jdbc.queryForObject("CALL create_mascota_privada_pr(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)",
                UUID.class, actorId, arreglo(organizaciones), clave, solicitud.nombre(), solicitud.especie(),
                solicitud.edadEstimadaMeses(), solicitud.tamano(), solicitud.energia(),
                arreglo(solicitud.comportamientoIds()), solicitud.organizacionId());
        return resultado(id, actorId, organizaciones);
    }

    // Una mascota ajena y una inexistente producen el mismo Optional vacío.
    public Optional<Mascota> consultar(UUID id, UUID actorId, Set<UUID> organizaciones) {
        return jdbc.query("SELECT * FROM ret_id_mascota_autorizada_pr(?, ?, ?)", this::mapear,
                id, actorId, arreglo(organizaciones)).stream().findFirst();
    }

    public List<Mascota> listar(UUID actorId, Set<UUID> organizaciones, UUID organizacionId,
                               Boolean activas, int pagina, int limite) {
        return jdbc.query("SELECT * FROM ret_all_mascota_pr(?, ?, ?, ?, ?, ?)", this::mapear,
                actorId, arreglo(organizaciones), organizacionId, activas, pagina, limite);
    }

    public long contar(UUID actorId, Set<UUID> organizaciones, UUID organizacionId, Boolean activas) {
        Long total = jdbc.queryForObject("SELECT count_mascota_pr(?, ?, ?, ?)", Long.class,
                actorId, arreglo(organizaciones), organizacionId, activas);
        return total == null ? 0 : total;
    }

    // La transacción conserva el bloqueo hasta leer la respuesta de nuestra edición.
    @Transactional
    public Mascota editar(UUID id, UUID actorId, Set<UUID> organizaciones, EditarMascotaSolicitud solicitud) {
        UUID actualizado = jdbc.queryForObject("CALL update_mascota_privada_pr(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)",
                UUID.class, id, actorId, arreglo(organizaciones), solicitud.nombre(), solicitud.especie(),
                solicitud.edadEstimadaMeses(), solicitud.tamano(), solicitud.energia(),
                arreglo(solicitud.comportamientoIds()), solicitud.versionEsperada());
        return resultado(actualizado, actorId, organizaciones);
    }

    @Transactional
    public Mascota desactivar(UUID id, UUID actorId, Set<UUID> organizaciones, UUID clave,
                             DesactivarMascotaSolicitud solicitud) {
        UUID actualizado = jdbc.queryForObject("CALL deactivate_mascota_pr(?, ?, ?, ?, ?, ?, NULL)", UUID.class,
                id, actorId, arreglo(organizaciones), clave, solicitud.motivo(), solicitud.versionEsperada());
        return resultado(actualizado, actorId, organizaciones);
    }

    private Mascota resultado(UUID id, UUID actorId, Set<UUID> organizaciones) {
        return consultar(id, actorId, organizaciones)
                .orElseThrow(() -> new DataRetrievalFailureException("La operación no devolvió un perfil accesible."));
    }

    private SqlArrayValue arreglo(java.util.Collection<UUID> valores) {
        return new SqlArrayValue("uuid", valores.toArray());
    }

    /** Traduce las columnas SQL a objetos Java; no decide permisos ni escribe datos. */
    private Mascota mapear(ResultSet fila, int indice) throws SQLException {
        Especie especie = new Especie(fila.getObject("especie_id", UUID.class), fila.getString("especie"),
                fila.getString("especie_nombre"), fila.getBoolean("especie_activa"));
        Object[] ids = (Object[]) fila.getArray("comportamiento_ids").getArray();
        Object[] nombres = (Object[]) fila.getArray("comportamiento_nombres").getArray();
        Object[] activas = (Object[]) fila.getArray("comportamiento_activas").getArray();
        List<Comportamiento> etiquetas = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            etiquetas.add(new Comportamiento((UUID) ids[i], (String) nombres[i], (Boolean) activas[i]));
        }
        return new Mascota(fila.getObject("id", UUID.class), fila.getTimestamp("creado_en").toInstant(),
                fila.getInt("version"), fila.getString("nombre"), especie, fila.getInt("edad_estimada_meses"),
                fila.getString("tamano"), fila.getString("energia"),
                fila.getObject("responsable_usuario_id", UUID.class), fila.getObject("organizacion_id", UUID.class),
                fila.getString("disponibilidad"), fila.getBoolean("activa"), etiquetas);
    }
}
