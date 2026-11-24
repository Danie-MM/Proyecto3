package cr.petmatchcr.mascotas.clinica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

/** HTTP y PostgreSQL reales. Solo el rol veterinario y la auditoría pendientes de A-03 usan un adaptador. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class B03ClinicaIntegrationTest {
    private static final String API = "/api/v1/";
    @Autowired MockMvc http;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @MockitoBean AccesoClinico acceso;
    private UUID titular;
    private UUID veterinario;
    private UUID ajeno;
    private UUID mascota;
    private UUID otra;

    @BeforeEach
    void preparar() {
        assertThat(jdbc.queryForObject("select current_schema()", String.class)).startsWith("pruebas");
        titular = cuenta();
        veterinario = cuenta();
        ajeno = cuenta();
        mascota = mascota(titular);
        otra = mascota(titular);
        when(acceso.esVeterinarioHabilitado(veterinario)).thenReturn(true);
    }

    @AfterEach
    void limpiarDatosPropios() {
        if (titular == null) return;
        jdbc.update("DELETE FROM operacion_clinica WHERE actor_id IN (?,?,?)", titular, veterinario, ajeno);
        jdbc.update("DELETE FROM evento_veterinario WHERE mascota_id IN (?,?)", mascota, otra);
        jdbc.update("DELETE FROM permiso_clinico WHERE mascota_id IN (?,?)", mascota, otra);
        jdbc.update("DELETE FROM mascota WHERE id IN (?,?)", mascota, otra);
        jdbc.update("DELETE FROM usuarios WHERE id_publico IN (?,?,?)", titular, veterinario, ajeno);
    }

    @Test
    void titularRegistraReintentaYConsultaEnOrdenConFiltros() throws Exception {
        var ruta = eventos(mascota);
        leer(titular, ruta).andExpect(jsonPath("$.totalElementos").value(0));
        var clave = UUID.randomUUID();
        var primera = vacuna("Refuerzo", "2020-01-31");
        var resultado = enviar(titular, ruta, primera, clave).andExpect(status().isCreated())
                .andExpect(jsonPath("$.autorUsuarioId").value(titular.toString()))
                .andExpect(jsonPath("$.origen").value("DECLARADO_RESPONSABLE"))
                .andExpect(header().string("Cache-Control", "no-store"));
        UUID id = id(resultado);
        resultado.andExpect(header().string("Location", API + "eventos-veterinarios/" + id));
        enviar(titular, ruta, primera, clave).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id.toString()));
        enviar(titular, ruta, vacuna("Otra", "2020-01-31"), clave).andExpect(status().isConflict());
        enviar(titular, ruta, vacuna("Más reciente", "2020-04-02"), UUID.randomUUID()).andExpect(status().isCreated());
        leer(titular, ruta + "?tipo=VACUNA&desde=2020-01-31&hasta=2020-04-02&limite=1")
                .andExpect(jsonPath("$.totalElementos").value(2)).andExpect(jsonPath("$.elementos[0].nombre").value("Más reciente"));
        leer(titular, ruta + "?desde=2020-01-31&hasta=2020-01-31")
                .andExpect(jsonPath("$.totalElementos").value(1)).andExpect(jsonPath("$.elementos[0].id").value(id.toString()));
        leer(titular, ruta + "?desde=2020-03-01&hasta=2020-01-01").andExpect(status().isBadRequest());
        leer(ajeno, API + "eventos-veterinarios/" + id).andExpect(status().isNotFound()).andExpect(jsonPath("$.nombre").doesNotExist());
    }

    @Test
    void validaTipoFechaTextosYDatosQueSoloPuedeFijarElServidor() throws Exception {
        String ruta = eventos(mascota);
        String fechaFutura = LocalDate.now(ZoneId.of("America/Costa_Rica")).plusDays(1).toString();
        enviar(titular, ruta, vacuna("A", fechaFutura), UUID.randomUUID()).andExpect(status().isBadRequest());
        enviar(titular, ruta, vacuna(" ", "2020-01-01"), UUID.randomUUID()).andExpect(status().isBadRequest());
        enviar(titular, ruta, vacuna("a".repeat(2001), "2020-01-01"), UUID.randomUUID()).andExpect(status().isBadRequest());
        enviar(titular, ruta, Map.of("tipo","MEDICAMENTO","nombre","X","fecha","2020-01-01"), UUID.randomUUID())
                .andExpect(status().isBadRequest());
        enviar(titular, ruta, Map.of("tipo","VACUNA","nombre","X","fecha","2020-01-01","origen","REGISTRADO_VETERINARIO"),
                UUID.randomUUID()).andExpect(status().isBadRequest());
        leer(titular, ruta).andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void veterinarioConLecturaNoEscribeYElPermisoEsPorMascota() throws Exception {
        leer(veterinario, eventos(mascota)).andExpect(status().isNotFound());
        enviar(ajeno, permisos(mascota), concesion("LECTURA"), UUID.randomUUID()).andExpect(status().isNotFound());
        when(acceso.esVeterinarioHabilitado(veterinario)).thenReturn(false);
        enviar(titular, permisos(mascota), concesion("LECTURA"), UUID.randomUUID()).andExpect(status().isBadRequest());
        when(acceso.esVeterinarioHabilitado(veterinario)).thenReturn(true);
        UUID clave = UUID.randomUUID();
        UUID id = id(enviar(titular, permisos(mascota), concesion("LECTURA"), clave).andExpect(status().isCreated()));
        enviar(titular, permisos(mascota), concesion("LECTURA"), clave).andExpect(jsonPath("$.id").value(id.toString()));
        enviar(titular, permisos(mascota), concesion("LECTURA_ESCRITURA"), clave).andExpect(status().isConflict());
        enviar(titular, permisos(mascota), concesion("LECTURA"), UUID.randomUUID()).andExpect(status().isConflict());
        verify(acceso, times(1)).auditarPermiso(AccesoClinico.Accion.CONCEDER_PERMISO_CLINICO, titular, id, mascota);
        leer(veterinario, eventos(mascota)).andExpect(status().isOk());
        leer(veterinario, eventos(otra)).andExpect(status().isNotFound());
        enviar(veterinario, eventos(mascota), vacuna("Ejemplo", "2020-01-01"), UUID.randomUUID()).andExpect(status().isForbidden());
        when(acceso.esVeterinarioHabilitado(veterinario)).thenReturn(false);
        leer(veterinario, eventos(mascota)).andExpect(status().isNotFound());
    }

    @Test
    void revocacionSeAplicaTambienAlReenvioYAlDetalleYaAbierto() throws Exception {
        UUID p = id(enviar(titular, permisos(mascota), concesion("LECTURA_ESCRITURA"), UUID.randomUUID()));
        var entrada = vacuna("Vacuna de ejemplo", "2021-02-22");
        var claveEvento = UUID.randomUUID();
        UUID e = id(enviar(veterinario, eventos(mascota), entrada, claveEvento).andExpect(status().isCreated())
                .andExpect(jsonPath("$.origen").value("REGISTRADO_VETERINARIO"))
                .andExpect(jsonPath("$.autorUsuarioId").value(veterinario.toString())));
        String detalle = API + "eventos-veterinarios/" + e;
        leer(veterinario, detalle).andExpect(status().isOk());
        String revocacion = API + "permisos-clinicos/" + p + "/revocacion";
        enviar(titular, revocacion, Map.of("motivo", "Cambio", "versionEsperada", 99), UUID.randomUUID())
                .andExpect(status().isConflict());
        var clave = UUID.randomUUID();
        var motivo = Map.of("motivo", "Fin de atención", "versionEsperada", 0);
        enviar(titular, revocacion, motivo, clave).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        enviar(titular, revocacion, motivo, clave).andExpect(status().isOk());
        verify(acceso, times(1)).auditarPermiso(AccesoClinico.Accion.REVOCAR_PERMISO_CLINICO, titular, p, mascota);
        leer(veterinario, detalle).andExpect(status().isNotFound());
        enviar(veterinario, eventos(mascota), entrada, claveEvento).andExpect(status().isNotFound());
        leer(titular, detalle).andExpect(status().isOk());
        leer(titular, permisos(mascota)).andExpect(jsonPath("$.totalElementos").value(0));
        leer(titular, permisos(mascota) + "?soloActivos=false").andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void unFalloDeAuditoriaRevierteElPermisoYLaReservaDeLaClave() throws Exception {
        doThrow(new DataAccessResourceFailureException("fallo simulado")).doNothing().when(acceso)
                .auditarPermiso(eq(AccesoClinico.Accion.CONCEDER_PERMISO_CLINICO), eq(titular), any(), eq(mascota));
        UUID clave = UUID.randomUUID();
        enviar(titular, permisos(mascota), concesion("LECTURA"), clave).andExpect(status().isServiceUnavailable());
        leer(titular, permisos(mascota) + "?soloActivos=false").andExpect(jsonPath("$.totalElementos").value(0));
        UUID p = id(enviar(titular, permisos(mascota), concesion("LECTURA"), clave).andExpect(status().isCreated()));
        doThrow(new DataAccessResourceFailureException("fallo simulado")).when(acceso)
                .auditarPermiso(AccesoClinico.Accion.REVOCAR_PERMISO_CLINICO, titular, p, mascota);
        enviar(titular, API + "permisos-clinicos/" + p + "/revocacion", Map.of("motivo","Prueba","versionEsperada",0),
                UUID.randomUUID()).andExpect(status().isServiceUnavailable());
        leer(veterinario, eventos(mascota)).andExpect(status().isOk());
    }

    private UUID cuenta() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuarios(id_publico,nombre_completo,email,rol) VALUES (?, 'Prueba B03', ?, 'ADOPTANTE')",
                id, correo(id));
        return id;
    }

    private UUID mascota(UUID actor) {
        return jdbc.queryForObject("""
            INSERT INTO mascota(nombre,especie,edad_estimada_meses,tamano,energia,responsable_usuario_id)
            VALUES ('Prueba B03','PERRO',2,'MEDIANO','MEDIA',?) RETURNING id
            """, UUID.class, actor);
    }

    private UUID id(ResultActions respuesta) throws Exception {
        return UUID.fromString(json.readTree(respuesta.andReturn().getResponse().getContentAsByteArray()).get("id").asString());
    }

    private static String correo(UUID actor) { return actor + "@example.test"; }
    private static String eventos(UUID id) { return API + "mascotas/" + id + "/eventos-veterinarios"; }
    private static String permisos(UUID id) { return API + "mascotas/" + id + "/permisos-clinicos"; }
    private static Map<String, String> vacuna(String nombre, String fecha) {
        return Map.of("tipo","VACUNA","nombre",nombre,"fecha",fecha);
    }
    private Map<String, Object> concesion(String alcance) {
        return Map.of("fundamento","VETERINARIO_AUTORIZADO","alcance",alcance,"beneficiarioUsuarioId",veterinario);
    }
    private ResultActions leer(UUID actor, String ruta) throws Exception {
        return http.perform(get(ruta).with(user(correo(actor))));
    }
    private ResultActions enviar(UUID actor, String ruta, Object datos, UUID clave) throws Exception {
        return http.perform(post(ruta).with(user(correo(actor))).with(csrf()).contentType("application/json")
                .header("Idempotency-Key", clave).content(json.writeValueAsBytes(datos)));
    }
}
