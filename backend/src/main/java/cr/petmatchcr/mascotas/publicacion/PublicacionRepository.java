package cr.petmatchcr.mascotas.publicacion;

import cr.petmatchcr.mascotas.dto.MascotaPublicaRespuesta;
import cr.petmatchcr.mascotas.dto.FiltrosCatalogoPublico;
import cr.petmatchcr.mascotas.service.ContextoMascotas.Actor;
import cr.petmatchcr.mascotas.service.ErrorMascota;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.SqlArrayValue;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
class PublicacionRepository {
    private final JdbcTemplate jdbc;

    PublicacionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    record Estado(int version, boolean activa, String disponibilidad, UUID organizacion) {}
    record Perfil(UUID id, String nombre, String especie, int edad, String tamano, String energia,
                  UUID organizacion, boolean foto, List<MascotaPublicaRespuesta.Etiqueta> etiquetas) {}
    record Foto(byte[] bytes, String mime) {}
    record FotoPublica(UUID organizacion, Foto archivo) {}
    record Recibo(UUID mascota, int versionEsperada, byte[] huella, UUID archivo, String mime, Integer tamano) {}

    Estado bloquearAutorizada(UUID id, Actor actor) {
        return jdbc.query("""
                SELECT * FROM ret_id_mascota_para_publicacion_pr(?,?,?)
                """, (r,n) -> new Estado(r.getInt("version"), r.getBoolean("activa"),
                r.getString("disponibilidad"), r.getObject("organizacion_id", UUID.class)),
                id, actor.usuarioId(), new SqlArrayValue("uuid", actor.organizaciones().toArray()))
                .stream().findFirst().orElseThrow(PublicacionRepository::ausente);
    }

    void disponibilidad(UUID id, String valor) {
        jdbc.update("CALL update_mascota_disponibilidad_pr(?,?)", id, valor);
    }

    Optional<Perfil> publicable(UUID id) {
        return jdbc.query("SELECT * FROM ret_id_mascota_publicable_pr(?)", this::leerPerfil, id).stream().findFirst();
    }

    private Perfil leerPerfil(ResultSet r, int indice) throws SQLException {
        UUID[] ids = (UUID[]) r.getArray("comportamiento_ids").getArray();
        String[] nombres = (String[]) r.getArray("comportamiento_nombres").getArray();
        var etiquetas = new ArrayList<MascotaPublicaRespuesta.Etiqueta>();
        for (int i = 0; i < ids.length; i++) {
            etiquetas.add(new MascotaPublicaRespuesta.Etiqueta(ids[i], nombres[i]));
        }
        return new Perfil(r.getObject("id", UUID.class), r.getString("nombre"), r.getString("especie"),
                r.getInt("edad_estimada_meses"), r.getString("tamano"), r.getString("energia"),
                r.getObject("organizacion_id", UUID.class), r.getBoolean("foto"), etiquetas);
    }

    List<UUID> organizacionesCandidatas() {
        return jdbc.queryForList("""
                SELECT DISTINCT organizacion_id FROM mascota_perfil_publicable_v WHERE especie IN ('PERRO','GATO')
                """, UUID.class);
    }

    boolean etiquetasActivas(Set<UUID> ids) {
        return ids.isEmpty() || jdbc.queryForObject("""
                SELECT count(*) FROM ret_all_etiqueta_comportamiento_pr() WHERE activa AND id = ANY(?)
                """, Long.class, new SqlArrayValue("uuid", ids.toArray())) == ids.size();
    }

    private static final String CATALOGO = " FROM ret_all_mascota_catalogo_pr(?,?,?,?,?,?,?,?)";

    private List<Object> parametrosCatalogo(FiltrosCatalogoPublico f, Set<UUID> organizaciones) {
        var args = new ArrayList<Object>();
        args.add(new SqlArrayValue("uuid", organizaciones.toArray()));
        args.add(f.nombre());
        args.add(f.especie());
        args.add(f.edadMinimaMeses());
        args.add(f.edadMaximaMeses());
        args.add(new SqlArrayValue("text", f.tamanos().toArray()));
        args.add(new SqlArrayValue("text", f.energias().toArray()));
        args.add(new SqlArrayValue("uuid", f.comportamientoIds().toArray()));
        return args;
    }

    long contar(FiltrosCatalogoPublico f, Set<UUID> organizaciones) {
        return jdbc.queryForObject("SELECT count(*)" + CATALOGO, Long.class, parametrosCatalogo(f, organizaciones).toArray());
    }

    List<Perfil> pagina(FiltrosCatalogoPublico f, Set<UUID> organizaciones, int pagina, int limite) {
        var args = parametrosCatalogo(f, organizaciones);
        args.add(limite);
        args.add((long) pagina * limite);
        return jdbc.query("SELECT *" + CATALOGO + " ORDER BY lower(nombre),id LIMIT ? OFFSET ?",
                this::leerPerfil, args.toArray());
    }

    Foto fotoPrivada(UUID mascota, Actor actor) {
        return jdbc.query("""
                SELECT * FROM ret_id_archivo_mascota_privado_pr(?,?,?)
                """, (r,n) -> new Foto(r.getBytes("contenido"), r.getString("mime_verificado")), mascota,
                actor.usuarioId(), new SqlArrayValue("uuid", actor.organizaciones().toArray()))
                .stream().findFirst().orElseThrow(PublicacionRepository::ausente);
    }

    FotoPublica fotoPublica(UUID mascota) {
        return jdbc.query("SELECT * FROM ret_id_archivo_mascota_publico_pr(?)", (r,n) ->
                new FotoPublica(r.getObject("organizacion_id", UUID.class),
                        new Foto(r.getBytes("contenido"), r.getString("mime_verificado"))), mascota)
                .stream().findFirst().orElseThrow(PublicacionRepository::ausente);
    }

    Optional<Recibo> reservarClave(UUID actor, UUID clave, Recibo actual) {
        Boolean nueva = jdbc.queryForObject("SELECT create_operacion_foto_pr(?,?,?,?,?,?,?,?)",
                Boolean.class, actor, clave, actual.mascota(), actual.versionEsperada(), actual.huella(),
                actual.archivo(), actual.mime(), actual.tamano());
        if (Boolean.TRUE.equals(nueva)) return Optional.empty();
        return Optional.of(jdbc.queryForObject("""
                SELECT * FROM ret_id_operacion_foto_pr(?,?)
                """, (r,n) -> new Recibo(r.getObject("mascota_id", UUID.class), r.getInt("version_esperada"),
                r.getBytes("huella"), r.getObject("foto_id", UUID.class), r.getString("mime"),
                r.getObject("tamano", Integer.class)), actor, clave));
    }

    void reemplazar(Recibo recibo, UUID actor, FotografiaVerificada nueva) {
        jdbc.update("CALL update_archivo_mascota_pr(?,?,?,?,?,?::bytea)", recibo.mascota(), actor,
                recibo.archivo(), recibo.mime(), recibo.huella(), nueva == null ? null : nueva.contenido());
    }

    static ErrorMascota ausente() {
        return new ErrorMascota(404, "NO_ENCONTRADO", "No encontramos el recurso solicitado.");
    }
}
