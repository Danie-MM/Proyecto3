package cr.petmatchcr.acceso;

import cr.petmatchcr.acceso.entities.RolUsuario;
import cr.petmatchcr.acceso.entities.Usuario;
import cr.petmatchcr.acceso.repositories.UsuarioRepository;
import cr.petmatchcr.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Verifica la frontera real A -> B sin simular el puerto de identidad ni llamar a Google. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccesoMascotasIntegrationTest {
    @Autowired MockMvc http;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper json;

    @Test
    void elJwtDeAEntregaA_BLaIdentidadPublicaPersistida() throws Exception {
        Usuario usuario = crearUsuario();
        String token = jwt.generarToken(usuario.getEmail(), usuario.getRol().name());
        http.perform(get("/api/v1/mascotas/contexto").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(usuario.getIdPublico().toString()))
                .andExpect(jsonPath("$.organizaciones.length()").value(0));
    }

    @Test
    void desactivarLaCuentaRevocaElAccesoAunqueElJwtNoHayaVencido() throws Exception {
        Usuario usuario = crearUsuario();
        String token = jwt.generarToken(usuario.getEmail(), usuario.getRol().name());
        usuario.setActivo(false);
        usuarios.saveAndFlush(usuario);
        http.perform(get("/api/v1/mascotas/contexto").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unBearerInvalidoNoReutilizaUnaAutenticacionPrevia() throws Exception {
        Usuario usuario = crearUsuario();
        http.perform(get("/api/v1/mascotas/contexto").with(user(usuario.getEmail()))
                        .header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elBearerValidoPrevaleceSobreOtraSesion() throws Exception {
        Usuario anterior = crearUsuario();
        Usuario actual = crearUsuario();
        String token = jwt.generarToken(actual.getEmail(), actual.getRol().name());
        http.perform(get("/api/v1/mascotas/contexto").with(user(anterior.getEmail()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(actual.getIdPublico().toString()));
    }

    @Test
    void laSesionCsrfNoGuardaLaAutenticacionDelJwt() throws Exception {
        Usuario usuario = crearUsuario();
        var respuesta = http.perform(get("/api/v1/sesion/csrf"))
                .andExpect(status().isOk()).andReturn();
        var sesion = (MockHttpSession) respuesta.getRequest().getSession(false);
        assertThat(sesion).isNotNull();
        http.perform(get("/api/v1/mascotas/contexto").session(sesion)
                        .header("Authorization", "Bearer " + jwt.generarToken(usuario.getEmail(), usuario.getRol().name())))
                .andExpect(status().isOk());
        http.perform(get("/api/v1/mascotas/contexto").session(sesion))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void csrfPublicoEntregaUnTokenQueSoloSirveConSuSesion() throws Exception {
        var respuesta = http.perform(get("/api/v1/sesion/csrf"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andReturn();
        String token = json.readTree(respuesta.getResponse().getContentAsString()).get("token").asText();
        var sesion = (MockHttpSession) respuesta.getRequest().getSession(false);
        String cuerpo = "{\"texto\":\"integracion csrf\"}";
        http.perform(post("/api/v1/pruebas-entorno").session(sesion)
                        .header("X-CSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());
        http.perform(post("/api/v1/pruebas-entorno")
                        .header("X-CSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
    }

    @Test
    void lasEscriturasDeAuthTambienExigenCsrfAntesDeValidarGoogle() throws Exception {
        http.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        http.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    private Usuario crearUsuario() {
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto("Responsable Integracion");
        usuario.setEmail("integracion-" + UUID.randomUUID() + "@example.test");
        usuario.setRol(RolUsuario.ADOPTANTE);
        return usuarios.saveAndFlush(usuario);
    }
}
