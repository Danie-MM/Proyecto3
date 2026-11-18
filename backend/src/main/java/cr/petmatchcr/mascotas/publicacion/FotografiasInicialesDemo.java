package cr.petmatchcr.mascotas.publicacion;

import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Completa los perfiles ficticios de Flyway con imágenes privadas, una sola vez. */
@Component
@Profile("local & demo-b01 & !integracion")
class FotografiasInicialesDemo implements ApplicationRunner {
    private record MascotaDemo(UUID id, String archivo) {}
    private static final List<MascotaDemo> MASCOTAS = List.of(
            new MascotaDemo(UUID.fromString("10000000-0000-4000-8000-000000000001"), "luna.jpg"),
            new MascotaDemo(UUID.fromString("10000000-0000-4000-8000-000000000002"), "sol.jpg"),
            new MascotaDemo(UUID.fromString("10000000-0000-4000-8000-000000000003"), "nube.jpg"),
            new MascotaDemo(UUID.fromString("10000000-0000-4000-8000-000000000004"), "coco.jpg"),
            new MascotaDemo(UUID.fromString("10000000-0000-4000-8000-000000000005"), "nube.jpg"),
            new MascotaDemo(UUID.fromString("10000000-0000-4000-8000-000000000006"), "sol.jpg"));

    private final JdbcTemplate jdbc;

    FotografiasInicialesDemo(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments argumentos) throws Exception {
        for (var mascota : MASCOTAS) {
            // El mismo bloqueo que usa la edición serializa dos arranques o una carga concurrente.
            var responsables = jdbc.queryForList(
                    "SELECT responsable_usuario_id FROM mascota WHERE id=? AND activa FOR UPDATE",
                    UUID.class, mascota.id());
            if (responsables.isEmpty()) continue;

            boolean yaGestionada = Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS (SELECT 1 FROM archivo_mascota WHERE mascota_id=?)
                        OR EXISTS (SELECT 1 FROM operacion_foto_mascota WHERE mascota_id=?)
                    """, Boolean.class, mascota.id(), mascota.id()));
            if (yaGestionada) continue;

            var recurso = new ClassPathResource("mascotas-demo/" + mascota.archivo());
            FotografiaVerificada foto;
            try (var entrada = recurso.getInputStream()) {
                foto = FotografiaVerificada.leer(entrada.readNBytes(FotografiaVerificada.LIMITE + 1));
            }
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(foto.contenido());
            jdbc.update("CALL update_archivo_mascota_pr(?,?,gen_random_uuid(),?,?,?::bytea)",
                    mascota.id(), responsables.getFirst(), foto.mime(), hash, foto.contenido());
        }
    }
}
