package cr.petmatchcr.mascotas.publicacion;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.datasource.hikari.minimum-idle=0",
        "spring.datasource.hikari.schema=pruebas_demo_b02_fotos",
        "spring.flyway.schemas=pruebas_demo_b02_fotos",
        "spring.flyway.default-schema=pruebas_demo_b02_fotos",
        "spring.jpa.properties.hibernate.default_schema=pruebas_demo_b02_fotos",
        "jwt.secret=cGV0bWF0Y2gtY2ktc29sby1wcnVlYmFzLTMya2V5eHg=",
        "google.client-id=pruebas-sin-google.apps.googleusercontent.com"
})
@ActiveProfiles({"local","demo-b01"})
@Transactional
class FotografiasInicialesDemoIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired FotografiasInicialesDemo fotos;

    @Test
    void seisPerfilesConFotosPrivadasPersistenSinCambiosTrasReiniciar() throws Exception {
        assertThat(jdbc.queryForObject("SELECT current_schema()",String.class)).isEqualTo("pruebas_demo_b02_fotos");
        String consulta = """
                SELECT m.id, m.version, a.id AS archivo, a.mime_verificado AS mime, a.sha256
                FROM mascota m JOIN archivo_mascota a ON a.mascota_id=m.id AND a.activa
                WHERE m.id IN ('10000000-0000-4000-8000-000000000001','10000000-0000-4000-8000-000000000002',
                               '10000000-0000-4000-8000-000000000003','10000000-0000-4000-8000-000000000004',
                               '10000000-0000-4000-8000-000000000005','10000000-0000-4000-8000-000000000006')
                AND a.subida_por_usuario_id=m.responsable_usuario_id
                AND a.sha256=sha256(a.contenido)
                ORDER BY m.id
                """;
        var inicial = jdbc.queryForList(consulta);
        assertThat(inicial).hasSize(6).allSatisfy(f -> assertThat(f.get("mime")).isEqualTo("image/jpeg"));
        assertThat(jdbc.queryForList("""
                SELECT m.nombre FROM mascota m JOIN archivo_mascota a ON a.mascota_id=m.id AND a.activa
                WHERE m.responsable_usuario_id='11111111-1111-4111-8111-111111111111'
                """, String.class)).contains("Luna","Milo","Kira");
        fotos.run(null);
        assertThat(jdbc.queryForList(consulta)).usingRecursiveComparison().isEqualTo(inicial);
    }

    @Test
    void unReemplazoYUnRetiroSeConservanAlReiniciar() throws Exception {
        UUID luna = UUID.fromString("10000000-0000-4000-8000-000000000001");
        var actor = jdbc.queryForObject("SELECT responsable_usuario_id FROM mascota WHERE id=?",UUID.class,luna);
        UUID archivoNuevo = UUID.randomUUID();
        byte[] contenido = FotografiaVerificadaTest.imagen("png");
        jdbc.update("CALL update_archivo_mascota_pr(?,?,?,?,sha256(?::bytea),?::bytea)",
                luna, actor, archivoNuevo, "image/png", contenido, contenido);
        fotos.run(null);
        assertThat(jdbc.queryForObject("SELECT id FROM archivo_mascota WHERE mascota_id=? AND activa",UUID.class,luna))
                .isEqualTo(archivoNuevo);

        jdbc.update("CALL update_archivo_mascota_pr(?,?,NULL,NULL,NULL,NULL)", luna,actor);
        var version = jdbc.queryForObject("SELECT version FROM mascota WHERE id=?",Integer.class,luna);
        fotos.run(null);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM archivo_mascota WHERE mascota_id=? AND activa",
                Integer.class, luna)).isZero();
        assertThat(jdbc.queryForObject("SELECT version FROM mascota WHERE id=?",Integer.class,luna)).isEqualTo(version);
    }
}
