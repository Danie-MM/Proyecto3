package cr.petmatchcr.acceso;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** La sesión ficticia sigue disponible exclusivamente en local + demo-b01. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "demo-b01"})
@TestPropertySource(locations = "classpath:application-test.properties", properties = {
        "spring.flyway.locations=classpath:db/migration",
        "spring.datasource.hikari.schema=${TEST_DEMO_DB_SCHEMA:pruebas_demo_abc}",
        "spring.flyway.schemas=${TEST_DEMO_DB_SCHEMA:pruebas_demo_abc}",
        "spring.flyway.default-schema=${TEST_DEMO_DB_SCHEMA:pruebas_demo_abc}",
        "spring.jpa.properties.hibernate.default_schema=${TEST_DEMO_DB_SCHEMA:pruebas_demo_abc}"
})
class DemoB01SeguridadIntegrationTest {
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;

    @Test
    void laDemoRequiereCsrfConservaSuSesionYNoOcultaUnJwtInvalido() throws Exception {
        String actor = "11111111-1111-4111-8111-111111111111";
        String cuerpo = "{\"actorId\":\"" + actor + "\"}";
        http.perform(get("/api/v1/demo-b01/actores")).andExpect(status().isOk());
        http.perform(post("/api/v1/demo-b01/sesion").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());

        var csrf = http.perform(get("/api/v1/sesion/csrf")).andExpect(status().isOk()).andReturn();
        String token = json.readTree(csrf.getResponse().getContentAsString()).get("token").asText();
        var sesion = (MockHttpSession) csrf.getRequest().getSession(false);
        http.perform(post("/api/v1/demo-b01/sesion").session(sesion).header("X-CSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk());
        http.perform(get("/api/v1/mascotas/contexto").session(sesion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuarioId").value(actor));
        http.perform(get("/api/v1/mascotas/contexto").session(sesion).header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());
    }
}
