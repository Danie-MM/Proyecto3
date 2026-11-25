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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Versión 01
 * Autora: Daniela Morales
 *
 * Descripción: Pruebas de organizaciones y permisos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrganizacionIntegrationTest {

    @Autowired MockMvc http;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper json;

    private final String ruta = "/api/v1/organizaciones";

    @Test
    void registrarOrganizacion() throws Exception {
        Usuario usuario = crearUsuario(RolUsuario.REFUGIO);

        http.perform(post(ruta).with(csrf())
                .header("Authorization", token(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Refugio de prueba\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Refugio de prueba"))
                .andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    void registrarSinPermiso() throws Exception {
        Usuario usuario = crearUsuario(RolUsuario.ADOPTANTE);

        http.perform(post(ruta).with(csrf())
                .header("Authorization", token(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Refugio de prueba\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void verificarAdministrador() throws Exception {
        Usuario usuario = crearUsuario(RolUsuario.REFUGIO);
        String id = crearOrganizacion(usuario);

        http.perform(get(ruta + "/" + id + "/miembros")
                .header("Authorization", token(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(usuario.getIdPublico().toString()))
                .andExpect(jsonPath("$[0].puedeAdministrar").value(true));
    }

    @Test
    void consultarOrganizaciones() throws Exception {
        Usuario usuario = crearUsuario(RolUsuario.REFUGIO);
        String id = crearOrganizacion(usuario);

        http.perform(get(ruta)
                .header("Authorization", token(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void consultarOrganizacionAjena() throws Exception {
        Usuario creador = crearUsuario(RolUsuario.REFUGIO);
        Usuario externo = crearUsuario(RolUsuario.ADOPTANTE);
        String id = crearOrganizacion(creador);

        http.perform(get(ruta + "/" + id)
                .header("Authorization", token(externo)))
                .andExpect(status().isForbidden());
    }

    @Test
    void consultarSinSesion() throws Exception {
        http.perform(get(ruta))
                .andExpect(status().isUnauthorized());
    }

    private Usuario crearUsuario(RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto("Usuario de prueba");
        usuario.setEmail("prueba-" + UUID.randomUUID() + "@example.test");
        usuario.setRol(rol);

        return usuarios.saveAndFlush(usuario);
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwt.generarToken(usuario.getEmail(), usuario.getRol().name());
    }

    private String crearOrganizacion(Usuario usuario) throws Exception {
        var respuesta = http.perform(post(ruta).with(csrf())
                .header("Authorization", token(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Refugio de prueba\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        return json.readTree(respuesta.getResponse().getContentAsString())
                .get("id").asText();
    }

    @Test
    void agregarMiembro() throws Exception {
        Usuario creador = crearUsuario(RolUsuario.REFUGIO);
        Usuario miembro = crearUsuario(RolUsuario.REFUGIO);
        String id = crearOrganizacion(creador);

        agregarUsuario(id, creador, miembro, false);

        http.perform(get(ruta + "/" + id)
                .header("Authorization", token(miembro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void cambiarPermiso() throws Exception {
        Usuario creador = crearUsuario(RolUsuario.REFUGIO);
        Usuario miembro = crearUsuario(RolUsuario.REFUGIO);
        String id = crearOrganizacion(creador);

        agregarUsuario(id, creador, miembro, false);

        http.perform(put(ruta + "/" + id + "/miembros/"
                + miembro.getIdPublico() + "/permiso").with(csrf())
                .header("Authorization", token(creador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"administrador\":true}"))
                .andExpect(status().isNoContent());

        http.perform(get(ruta + "/" + id + "/miembros")
                .header("Authorization", token(miembro)))
                .andExpect(status().isOk());
    }

    @Test
    void quitarMiembro() throws Exception {
        Usuario creador = crearUsuario(RolUsuario.REFUGIO);
        Usuario miembro = crearUsuario(RolUsuario.REFUGIO);
        String id = crearOrganizacion(creador);

        agregarUsuario(id, creador, miembro, false);

        http.perform(delete(ruta + "/" + id + "/miembros/"
                + miembro.getIdPublico()).with(csrf())
                .header("Authorization", token(creador)))
                .andExpect(status().isNoContent());

        http.perform(get(ruta + "/" + id)
                .header("Authorization", token(miembro)))
                .andExpect(status().isForbidden());
    }

    @Test
    void quitarUltimoAdministrador() throws Exception {
        Usuario creador = crearUsuario(RolUsuario.REFUGIO);
        String id = crearOrganizacion(creador);

        http.perform(delete(ruta + "/" + id + "/miembros/"
                + creador.getIdPublico()).with(csrf())
                .header("Authorization", token(creador)))
                .andExpect(status().isConflict());
    }

    @Test
    void cambiarPermisoUltimoAdministrador() throws Exception {
        Usuario creador = crearUsuario(RolUsuario.REFUGIO);
        String id = crearOrganizacion(creador);

        http.perform(put(ruta + "/" + id + "/miembros/"
                + creador.getIdPublico() + "/permiso").with(csrf())
                .header("Authorization", token(creador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"administrador\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    void agregarUltimoAdministradorSinPermiso() throws Exception {
        Usuario creador = crearUsuario(RolUsuario.REFUGIO);
        String id = crearOrganizacion(creador);

        String cuerpo = "{\"usuarioId\":\"" + creador.getIdPublico()
                + "\",\"administrador\":false}";

        http.perform(post(ruta + "/" + id + "/miembros").with(csrf())
                .header("Authorization", token(creador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isConflict());
    }

    private void agregarUsuario(String id, Usuario creador,
                                Usuario miembro, boolean administrador) throws Exception {

        String cuerpo = "{\"usuarioId\":\"" + miembro.getIdPublico()
                + "\",\"administrador\":" + administrador + "}";

        http.perform(post(ruta + "/" + id + "/miembros").with(csrf())
                .header("Authorization", token(creador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isNoContent());
    }

    @Test
    void consultarContextoAdministrador() throws Exception {
        Usuario creador = crearUsuario(RolUsuario.REFUGIO);
        crearOrganizacion(creador);

        http.perform(get("/api/v1/mascotas/contexto")
                .header("Authorization", token(creador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizaciones.length()").value(1));
    }

    @Test
    void consultarContextoMiembro() throws Exception {
        Usuario creador = crearUsuario(RolUsuario.REFUGIO);
        Usuario miembro = crearUsuario(RolUsuario.REFUGIO);
        String id = crearOrganizacion(creador);

        agregarUsuario(id, creador, miembro, false);

        http.perform(get(ruta + "/" + id + "/miembros")
                .header("Authorization", token(miembro)))
                .andExpect(status().isForbidden());

        http.perform(get("/api/v1/mascotas/contexto")
                .header("Authorization", token(miembro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizaciones.length()").value(0));
    }

    @Test
    void actualizarPermisosMascotas() throws Exception {
        Usuario creador = crearUsuario(RolUsuario.REFUGIO);
        Usuario miembro = crearUsuario(RolUsuario.REFUGIO);
        String id = crearOrganizacion(creador);

        agregarUsuario(id, creador, miembro, false);

        String jwtMiembro = token(miembro);
        String permiso = ruta + "/" + id + "/miembros/" + miembro.getIdPublico() + "/permiso";

        http.perform(get("/api/v1/mascotas/contexto")
                .header("Authorization", jwtMiembro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizaciones.length()").value(0));

        http.perform(put(permiso).with(csrf())
                .header("Authorization", token(creador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"administrador\":true}"))
                .andExpect(status().isNoContent());

        http.perform(get("/api/v1/mascotas/contexto")
                .header("Authorization", jwtMiembro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizaciones.length()").value(1));

        http.perform(put(permiso).with(csrf())
                .header("Authorization", token(creador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"administrador\":false}"))
                .andExpect(status().isNoContent());

        http.perform(get("/api/v1/mascotas/contexto")
                .header("Authorization", jwtMiembro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizaciones.length()").value(0));
    }
}