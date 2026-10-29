package cr.petmatchcr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Comprueba el contrato HTTP usando PostgreSQL real, en un esquema de pruebas. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://db:5432/petmatchcr?currentSchema=pruebas"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PruebaEntornoIntegrationTest {
    private static final String RUTA = "/api/v1/pruebas-entorno";
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void guardarYConsultarDevuelveElRegistroRealConTextoRecortado() throws Exception {
        JsonNode creado = crear("  Prueba de Natanael  ");
        UUID id = UUID.fromString(creado.get("id").asText());
        Instant.parse(creado.get("creadoEn").asText());
        assertThat(creado.get("texto").asText()).isEqualTo("Prueba de Natanael");

        String consultado = http.perform(get(RUTA + "/" + id))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(consultado)).isEqualTo(creado);
        assertThat(jdbc.queryForObject("select current_schema()", String.class)).isEqualTo("pruebas");
        assertThat(jdbc.queryForObject("select texto from prueba_entorno where id = ?", String.class, id))
            .isEqualTo("Prueba de Natanael");
    }

    @Test
    void aceptaOchentaCaracteresDespuesDeRecortarIncluyendoUnicode() throws Exception {
        String texto = "🐾".repeat(80);
        assertThat(crear("  " + texto + "  ").get("texto").asText()).isEqualTo(texto);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"texto\":null}", "{\"texto\":\"\"}",
        "{\"texto\":\"   \"}", "{\"texto\":123}", "{\"texto\":true}",
        "{\"texto\":{}}", "{\"texto\":[]}", "{", "[]", "{\"texto\":\"uno\"} {}"})
    void rechazaCuerposInvalidosSinInsertar(String cuerpo) throws Exception {
        long antes = jdbc.queryForObject("select count(*) from prueba_entorno", Long.class);
        String respuesta = http.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").isString())
            .andExpect(jsonPath("$.mensaje").isString())
            .andExpect(jsonPath("$.erroresCampos").isArray())
            .andReturn().getResponse().getContentAsString();
        UUID.fromString(json.readTree(respuesta).get("trazaId").asText());
        assertThat(jdbc.queryForObject("select count(*) from prueba_entorno", Long.class)).isEqualTo(antes);
    }

    @Test
    void rechazaMasDeOchentaCaracteres() throws Exception {
        http.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(java.util.Map.of("texto", "a".repeat(81)))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erroresCampos[0].campo").value("texto"));
    }

    @Test
    void distingueIdMalEscritoDeRegistroInexistente() throws Exception {
        http.perform(get(RUTA + "/no-es-uuid")).andExpect(status().isBadRequest());
        http.perform(get(RUTA + "/" + UUID.randomUUID()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensaje").value("No encontramos ese registro"));
    }

    @Test
    void saludNoExponeDetallesYNoSeAbrenLosOtrosModulos() throws Exception {
        http.perform(get("/actuator/health"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist());
        int estado = http.perform(get("/api/v1/mascotas")).andReturn().getResponse().getStatus();
        assertThat(estado).isIn(401, 403);
    }

    private JsonNode crear(String texto) throws Exception {
        String respuesta = http.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(java.util.Map.of("texto", texto))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta);
    }
}
