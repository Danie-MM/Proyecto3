package cr.petmatchcr.mascotas.clinica;

import static cr.petmatchcr.mascotas.clinica.DatosClinicos.*;

import cr.petmatchcr.mascotas.service.ContextoMascotas.Actor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.SqlArrayValue;
import org.springframework.stereotype.Repository;

@Repository
class RepositorioClinico {
    private final JdbcTemplate jdbc;

    RepositorioClinico(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    record MascotaClinica(boolean responsable, boolean activa, UUID organizacionId) { }
    record Cambio(UUID id, boolean aplicado) { }

    /** Las escrituras y revocaciones toman el mismo bloqueo antes de autorizar la operación. */
    Optional<MascotaClinica> mascota(UUID id, Actor actor, boolean escritura) {
        String consulta = """
            SELECT b_mascota_autorizada_pr(responsable_usuario_id, organizacion_id, ?, ?) AS responsable, activa, organizacion_id
            FROM mascota WHERE id = ?
            """ + (escritura ? " FOR UPDATE" : "");
        return jdbc.query(consulta, (r, n) -> new MascotaClinica(r.getBoolean("responsable"), r.getBoolean("activa"),
                        r.getObject("organizacion_id", UUID.class)),
                actor.usuarioId(), new SqlArrayValue("uuid", actor.organizaciones().toArray()), id)
                .stream().findFirst();
    }

    Optional<String> alcance(UUID mascota, UUID beneficiario) {
        return jdbc.query("SELECT alcance FROM permiso_clinico WHERE mascota_id=? AND beneficiario_usuario_id=? AND activo",
                (r, n) -> r.getString(1), mascota, beneficiario).stream().findFirst();
    }

    Cambio conceder(UUID mascota, UUID actor, Concesion solicitud, UUID clave) {
        return jdbc.queryForObject("CALL create_permiso_clinico_pr(?, ?, ?, ?, ?, NULL, NULL)",
                this::cambio, mascota, actor, solicitud.beneficiarioUsuarioId(), solicitud.alcance(), clave);
    }

    Cambio revocar(UUID id, UUID actor, String motivo, int version, UUID clave) {
        return jdbc.queryForObject("CALL deactivate_permiso_clinico_pr(?, ?, ?, ?, ?, NULL, NULL)",
                this::cambio, id, actor, motivo, version, clave);
    }

    private Cambio cambio(ResultSet r, int n) throws SQLException {
        return new Cambio(r.getObject("p_id", UUID.class), r.getBoolean("p_cambio"));
    }

    UUID registrar(UUID mascota, UUID actor, String origen, NuevaVacuna vacuna, UUID clave) {
        return jdbc.queryForObject("CALL create_evento_veterinario_pr(?, ?, ?, ?, ?, ?, ?, NULL)", UUID.class,
                mascota, actor, origen, vacuna.fecha(), vacuna.nombre(), vacuna.descripcion(), clave);
    }

    Optional<Evento> evento(UUID id) {
        return jdbc.query("SELECT * FROM ret_id_evento_veterinario_pr(?)", this::mapearEvento, id)
                .stream().findFirst();
    }

    Optional<Permiso> permiso(UUID id) {
        return jdbc.query("SELECT * FROM ret_id_permiso_clinico_pr(?)", this::mapearPermiso, id)
                .stream().findFirst();
    }

    Pagina<Evento> eventos(UUID mascota, LocalDate desde, LocalDate hasta, String tipo, int pagina, int limite) {
        String fuente = " FROM ret_all_evento_veterinario_pr(?, ?, ?, ?)";
        var datos = jdbc.query("SELECT *" + fuente + " ORDER BY fecha DESC, creado_en DESC, id DESC LIMIT ? OFFSET ?",
                this::mapearEvento, mascota, desde, hasta, tipo, limite, (long) pagina * limite);
        long total = jdbc.queryForObject("SELECT count(*)" + fuente, Long.class, mascota, desde, hasta, tipo);
        return new Pagina<>(datos, pagina, limite, total);
    }

    Pagina<Permiso> permisos(UUID mascota, boolean soloActivos, int pagina, int limite) {
        String fuente = " FROM ret_all_permiso_clinico_pr(?, ?)";
        var datos = jdbc.query("SELECT *" + fuente + " ORDER BY creado_en DESC, id DESC LIMIT ? OFFSET ?",
                this::mapearPermiso, mascota, soloActivos, limite, (long) pagina * limite);
        long total = jdbc.queryForObject("SELECT count(*)" + fuente, Long.class, mascota, soloActivos);
        return new Pagina<>(datos, pagina, limite, total);
    }

    private Evento mapearEvento(ResultSet r, int n) throws SQLException {
        return new Evento(r.getObject("id", UUID.class), r.getTimestamp("creado_en").toInstant(),
                r.getInt("version"), r.getObject("mascota_id", UUID.class), r.getString("tipo"),
                r.getObject("fecha", LocalDate.class), r.getObject("autor_usuario_id", UUID.class),
                r.getString("origen"), r.getString("nombre"), r.getString("descripcion"),
                r.getInt("numero_revision"), null, null, null, null, null, null);
    }

    private Permiso mapearPermiso(ResultSet r, int n) throws SQLException {
        Timestamp revocadoEn = r.getTimestamp("revocado_en");
        return new Permiso(r.getObject("id", UUID.class), r.getTimestamp("creado_en").toInstant(),
                r.getInt("version"), r.getObject("mascota_id", UUID.class), r.getObject("beneficiario_usuario_id", UUID.class),
                r.getString("fundamento"), r.getString("alcance"), r.getObject("otorgado_por_usuario_id", UUID.class),
                r.getBoolean("activo"), revocadoEn == null ? null : revocadoEn.toInstant(),
                r.getObject("revocado_por_usuario_id", UUID.class), r.getString("motivo_revocacion"));
    }
}
