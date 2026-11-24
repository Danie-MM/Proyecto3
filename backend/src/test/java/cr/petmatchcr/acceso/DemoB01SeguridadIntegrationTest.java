package cr.petmatchcr.acceso;

import cr.petmatchcr.acceso.entities.RolUsuario;
import cr.petmatchcr.acceso.entities.Usuario;
import cr.petmatchcr.acceso.repositories.UsuarioRepository;
import cr.petmatchcr.security.JwtService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;

    @Test
    @Transactional
    void elJwtRealPrevaleceSobreLaSesionDemoEnMascotasUbicacionYEntorno() throws Exception {
        Usuario cuenta = new Usuario();
        cuenta.setNombreCompleto("Prueba de navegación");
        cuenta.setEmail("sesion-" + UUID.randomUUID() + "@example.test");
        cuenta.setRol(RolUsuario.ADOPTANTE);
        usuarios.saveAndFlush(cuenta);
        String token = "Bearer " + jwt.generarToken(cuenta.getEmail(), cuenta.getRol().name());
        String titularReal = cuenta.getIdPublico().toString();
        String representanteDemo = "33333333-3333-4333-8333-333333333333";
        var seleccion = http.perform(post("/api/v1/demo-b01/sesion").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actorId\":\"" + representanteDemo + "\"}")).andExpect(status().isOk()).andReturn();
        var sesion = (MockHttpSession) seleccion.getRequest().getSession(false);

        http.perform(get("/api/v1/mascotas/contexto").session(sesion).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuarioId").value(titularReal))
                .andExpect(jsonPath("$.organizaciones.length()").value(0));
        http.perform(get("/api/extravios/ubicacion").session(sesion).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuarioId").value(titularReal))
                .andExpect(jsonPath("$.configurada").value(false));
        var guardado = http.perform(post("/api/v1/pruebas-entorno").session(sesion).header("Authorization", token)
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Prueba de navegación\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        http.perform(get(guardado).session(sesion).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.texto").value("Prueba de navegación"));

        http.perform(get("/api/v1/mascotas/contexto").session(sesion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuarioId").value(representanteDemo))
                .andExpect(jsonPath("$.organizaciones.length()").value(1));
        cuenta.setActivo(false);
        usuarios.saveAndFlush(cuenta);
        http.perform(get("/api/extravios/ubicacion").session(sesion).header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

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
