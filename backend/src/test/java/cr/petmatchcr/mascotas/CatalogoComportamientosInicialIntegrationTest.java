package cr.petmatchcr.mascotas;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogoComportamientosInicialIntegrationTest {
    @Autowired MockMvc http;
    @Autowired JdbcTemplate jdbc;

    @Test
    void ofreceComportamientosActivosSinHabilitarLaDemostracion() throws Exception {
        http.perform(get("/api/v1/catalogos/mascotas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comportamientos[?(@.activo == true)].nombre",
                        hasItems("Sociable", "Tranquilo", "Juguetón")));
    }

    @Test
    void repetirLaCargaNoDuplicaNiReactivaOpcionesRetiradas() {
        UUID sociable = jdbc.queryForObject(
                "SELECT id FROM etiqueta_comportamiento WHERE nombre = 'Sociable' ORDER BY id LIMIT 1",
                UUID.class);
        jdbc.update("UPDATE etiqueta_comportamiento SET activa = false WHERE id = ?", sociable);
        UUID personalizada = UUID.randomUUID();
        jdbc.update("INSERT INTO etiqueta_comportamiento(id, nombre, activa) VALUES (?, ?, true)",
                personalizada, "Comportamiento personalizado de prueba");
        Long cantidad = jdbc.queryForObject("SELECT count(*) FROM etiqueta_comportamiento", Long.class);

        jdbc.execute((ConnectionCallback<Void>) conexion -> {
            ScriptUtils.executeSqlScript(conexion, new EncodedResource(
                    new ClassPathResource("db/migration/V17__datos_iniciales_comportamientos.sql"), "UTF-8"));
            return null;
        });

        assertEquals(cantidad, jdbc.queryForObject("SELECT count(*) FROM etiqueta_comportamiento", Long.class));
        assertEquals(Boolean.FALSE, jdbc.queryForObject(
                "SELECT activa FROM etiqueta_comportamiento WHERE id = ?", Boolean.class, sociable));
        assertEquals(1L, jdbc.queryForObject(
                "SELECT count(*) FROM etiqueta_comportamiento WHERE id = ? AND activa", Long.class, personalizada));
    }
}
