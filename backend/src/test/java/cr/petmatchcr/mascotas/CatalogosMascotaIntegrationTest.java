package cr.petmatchcr.mascotas;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogosMascotaIntegrationTest {
    private static final String RUTA = "/api/v1/catalogos/mascotas";
    @Autowired MockMvc http;
    @Autowired JdbcTemplate jdbc;

    @Test
    void permiteConsultarElCatalogoVacioAntesDeAprobarEtiquetas() throws Exception {
        jdbc.update("DELETE FROM mascota_comportamiento");
        jdbc.update("DELETE FROM etiqueta_comportamiento");
        http.perform(get(RUTA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.especies", contains("GATO", "PERRO")))
            .andExpect(jsonPath("$.tamanos", contains("PEQUENO", "MEDIANO", "GRANDE")))
            .andExpect(jsonPath("$.energias", contains("BAJA", "MEDIA", "ALTA")))
            .andExpect(jsonPath("$.comportamientos", hasSize(0)))
            .andExpect(jsonPath("$.versionCatalogo").value("0.2"));
    }

    @Test
    void incluyeElIdentificadorNombreYVigenciaDeLasEtiquetas() throws Exception {
        UUID activa = UUID.randomUUID();
        UUID retirada = UUID.randomUUID();
        jdbc.update("INSERT INTO etiqueta_comportamiento (id, nombre, activa) VALUES (?, ?, ?), (?, ?, ?)",
                activa, "Etiqueta de prueba A", true, retirada, "Etiqueta de prueba B", false);
        String etiqueta = "$.comportamientos[?(@.id == '%s')]";
        http.perform(get(RUTA))
            .andExpect(status().isOk())
            .andExpect(jsonPath(etiqueta.formatted(activa) + ".nombre", contains("Etiqueta de prueba A")))
            .andExpect(jsonPath(etiqueta.formatted(activa) + ".activo", contains(true)))
            .andExpect(jsonPath(etiqueta.formatted(retirada) + ".nombre", contains("Etiqueta de prueba B")))
            .andExpect(jsonPath(etiqueta.formatted(retirada) + ".activo", contains(false)));
    }

    @Test
    void ofreceEspeciesNuevasDelCatalogoYOmiteLasRetiradas() throws Exception {
        jdbc.update("INSERT INTO especie(codigo, nombre, activa) VALUES "
                + "('CONEJO_PRUEBA_HTTP', 'Conejo ficticio', true), "
                + "('AVE_PRUEBA_HTTP', 'Ave ficticia retirada', false)");
        http.perform(get(RUTA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.especies", hasItem("CONEJO_PRUEBA_HTTP")))
            .andExpect(jsonPath("$.especies", not(hasItem("AVE_PRUEBA_HTTP"))));
    }

    @Test
    void laLecturaDelCatalogoNoAbreLaRutaPrivadaNiPermiteEscrituras() throws Exception {
        http.perform(get("/api/v1/mascotas")).andExpect(status().isUnauthorized());
        http.perform(post(RUTA)).andExpect(status().isForbidden());
    }
}
