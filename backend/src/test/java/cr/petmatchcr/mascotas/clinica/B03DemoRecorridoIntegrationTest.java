package cr.petmatchcr.mascotas.clinica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Usa el mismo selector, cuentas y adaptador clínico de la demostración. Cada prueba revierte sus escrituras. */
@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.datasource.hikari.minimum-idle=0",
        "spring.datasource.hikari.schema=${TEST_B03_DEMO_SCHEMA:pruebas_demo_b03}",
        "spring.flyway.schemas=${TEST_B03_DEMO_SCHEMA:pruebas_demo_b03}",
        "spring.flyway.default-schema=${TEST_B03_DEMO_SCHEMA:pruebas_demo_b03}",
        "spring.jpa.properties.hibernate.default_schema=${TEST_B03_DEMO_SCHEMA:pruebas_demo_b03}",
        "jwt.secret=cGV0bWF0Y2gtY2ktc29sby1wcnVlYmFzLTMya2V5eHg=",
        "google.client-id=pruebas-sin-google.apps.googleusercontent.com"
})
@ActiveProfiles({"local", "demo-b01"})
@AutoConfigureMockMvc
@Transactional
class B03DemoRecorridoIntegrationTest {
    private static final String TITULAR = "11111111-1111-4111-8111-111111111111";
    private static final String VETERINARIO = "55555555-5555-4555-8555-555555555555";
    private static final String LUNA = "/api/v1/mascotas/10000000-0000-4000-8000-000000000001";
    private static final UUID PERMISO_LECTURA_INICIAL = UUID.fromString("b0300000-0000-4000-8000-000000000102");
    @Autowired MockMvc http;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    @Test
    void concederRegistrarYRevocarEnElEsquemaAislado() throws Exception {
        assertThat(jdbc.queryForObject("SELECT current_schema()", String.class)).startsWith("pruebas_");
        http.perform(get("/api/v1/demo-b01/actores")).andExpect(jsonPath("$.actores.length()").value(5));
        var titular = ingresar(TITULAR);
        var veterinario = ingresar(VETERINARIO);
        http.perform(get(LUNA + "/eventos-veterinarios").session(veterinario)
                        .param("desde","2026-10-01").param("hasta","2026-10-31").param("tipo","VACUNA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.elementos[0].nombre").value("Refuerzo de triple felina"))
                .andExpect(jsonPath("$.elementos[0].origen").value("REGISTRADO_VETERINARIO"));
        var permisoInicial = http.perform(post("/api/v1/permisos-clinicos/" + PERMISO_LECTURA_INICIAL + "/revocacion")
                        .session(titular).with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json")
                        .content("{\"motivo\":\"Ensayar permiso de escritura\",\"versionEsperada\":0}"));
        permisoInicial.andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        var permiso = http.perform(post(LUNA + "/permisos-clinicos").session(titular).with(csrf())
                        .header("Idempotency-Key", UUID.randomUUID()).contentType("application/json").content("""
                            {"beneficiarioUsuarioId":"%s","alcance":"LECTURA_ESCRITURA","fundamento":"VETERINARIO_AUTORIZADO"}
                            """.formatted(VETERINARIO)))
                .andExpect(status().isCreated()).andReturn();
        UUID id = UUID.fromString(json.readTree(permiso.getResponse().getContentAsByteArray()).get("id").asString());
        http.perform(post(LUNA + "/eventos-veterinarios").session(veterinario).with(csrf())
                        .header("Idempotency-Key", UUID.randomUUID()).contentType("application/json")
                        .content("{\"tipo\":\"VACUNA\",\"nombre\":\"Ensayo local\",\"fecha\":\"2020-01-01\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.autorUsuarioId").value(VETERINARIO))
                .andExpect(jsonPath("$.origen").value("REGISTRADO_VETERINARIO"));
        http.perform(post("/api/v1/permisos-clinicos/" + id + "/revocacion").session(titular).with(csrf())
                        .header("Idempotency-Key",UUID.randomUUID()).contentType("application/json")
                        .content("{\"motivo\":\"Fin del ensayo\",\"versionEsperada\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        http.perform(get(LUNA + "/eventos-veterinarios").session(veterinario)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForList("SELECT accion FROM auditoria_permiso_clinico_demo WHERE permiso_id = ? ORDER BY accion",
                String.class, id)).containsExactly("CONCEDER_PERMISO_CLINICO","REVOCAR_PERMISO_CLINICO");
    }

    @Test
    void repetirLaCargaPreservaLaRevocacionLosEventosDelUsuarioYLosEjemplosUnicos() throws Exception {
        var titular = ingresar(TITULAR);
        http.perform(post("/api/v1/permisos-clinicos/" + PERMISO_LECTURA_INICIAL + "/revocacion").session(titular)
                        .with(csrf()).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json")
                        .content("{\"motivo\":\"Decisión de prueba\",\"versionEsperada\":0}"))
                .andExpect(status().isOk());
        var respuesta = http.perform(post(LUNA + "/eventos-veterinarios").session(titular).with(csrf())
                        .header("Idempotency-Key",UUID.randomUUID()).contentType("application/json")
                        .content("{\"tipo\":\"VACUNA\",\"nombre\":\"Anotación privada de QA\",\"fecha\":\"2026-05-01\"}"))
                .andExpect(status().isCreated()).andReturn();
        String muestra = new ClassPathResource("db/demo-b03/R__expedientes_de_ejemplo.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        jdbc.execute(muestra);
        jdbc.execute(muestra);
        var idQA = json.readTree(respuesta.getResponse().getContentAsByteArray()).get("id").asString();
        http.perform(get("/api/v1/eventos-veterinarios/" + idQA).session(titular))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Anotación privada de QA"));
        http.perform(get(LUNA + "/permisos-clinicos").session(titular)).andExpect(jsonPath("$.totalElementos").value(0));
        http.perform(get(LUNA + "/eventos-veterinarios").session(ingresar(VETERINARIO))).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM evento_veterinario WHERE id BETWEEN ? AND ?", Integer.class,
                        UUID.fromString("b0300000-0000-4000-8000-000000000001"),
                        UUID.fromString("b0300000-0000-4000-8000-000000000006"))).isEqualTo(6);
    }

    private MockHttpSession ingresar(String id) throws Exception {
        var res = http.perform(post("/api/v1/demo-b01/sesion").with(csrf()).contentType("application/json")
                        .content("{\"actorId\":\"" + id + "\"}")).andExpect(status().isOk()).andReturn();
        return (MockHttpSession) res.getRequest().getSession(false);
    }
}
