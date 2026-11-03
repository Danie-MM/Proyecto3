package cr.petmatchcr;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EntornoPostgresTest {
    private static final String RUTA = "/api/v1/pruebas-entorno";

    @Autowired MockMvc http;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    record Registro(UUID id, String texto, String creadoEn) {}

    @ParameterizedTest
    @ValueSource(ints = {1, 80})
    void guardaYRecuperaLosLimitesEnPostgres(int longitud) throws Exception {
        String texto = "🐾".repeat(longitud);
        String solicitud = json.writeValueAsString(Map.of("texto", "  " + texto + "  "));
        String cuerpo = http.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(solicitud))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Registro creado = json.readValue(cuerpo, Registro.class);
        assertThat(creado.texto()).isEqualTo(texto);
        assertThat(creado.id()).isNotNull();
        assertThat(creado.creadoEn()).endsWith("Z");
        Instant.parse(creado.creadoEn());
        assertThat(jdbc.queryForObject("select current_schema()", String.class)).isEqualTo("pruebas");
        assertThat(jdbc.queryForObject("select texto from prueba_entorno where id = ?", String.class, creado.id()))
                .isEqualTo(texto);

        String respuesta = http.perform(get(RUTA + "/{id}", creado.id())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(json.readValue(respuesta, Registro.class)).isEqualTo(creado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"texto\":null}", "{\"texto\":\"  \"}", "{\"texto\":false}",
            "{\"texto\":42}", "[]", "{", "{\"texto\":\"prueba\"} {}"})
    void rechazaJsonInvalidoSinGuardar(String cuerpo) throws Exception {
        Integer antes = jdbc.queryForObject("select count(*) from prueba_entorno", Integer.class);
        http.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").isString())
                .andExpect(jsonPath("$.mensaje").isString()).andExpect(jsonPath("$.erroresCampos").isArray())
                .andExpect(jsonPath("$.trazaId").isString());
        assertThat(jdbc.queryForObject("select count(*) from prueba_entorno", Integer.class)).isEqualTo(antes);
    }

    @Test
    void rechazaOchentaYUnCaracteres() throws Exception {
        String cuerpo = json.writeValueAsString(Map.of("texto", "a".repeat(81)));
        http.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erroresCampos[0].campo").value("texto"));
    }

    @Test
    void distingueUuidInvalidoDeUnRegistroInexistente() throws Exception {
        http.perform(get(RUTA + "/1-1-1-1-1")).andExpect(status().isBadRequest());
        http.perform(get(RUTA + "/{id}", UUID.randomUUID())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No encontramos ese registro"));
    }

    @Test
    void estadoDeSaludPublicoYRutasDeNegocioCerradasParaAnonimos() throws Exception {
        http.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP")).andExpect(jsonPath("$.components").doesNotExist());
        int respuesta = http.perform(get("/api/v1/mascotas")).andReturn().getResponse().getStatus();
        assertThat(respuesta).isIn(401, 403);
    }
}
