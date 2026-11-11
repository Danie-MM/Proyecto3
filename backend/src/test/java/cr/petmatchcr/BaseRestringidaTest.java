package cr.petmatchcr;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("restringido")
@TestPropertySource(locations = "classpath:application-test.properties")
class BaseRestringidaTest {
    @Autowired RequestMappingHandlerMapping requestMappingHandlerMapping;
    @Autowired MockMvc http;

    @Test
    void fueraDelEntornoLocalLaRutaDePruebaQuedaInhabilitada() throws Exception {
        var rutas = requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPatternValues().stream()).toList();
        assertThat(rutas).noneMatch(ruta -> ruta.startsWith("/api/v1/pruebas-entorno"));
        int estado = http.perform(get("/api/v1/pruebas-entorno/11111111-1111-4111-8111-111111111111"))
                .andReturn().getResponse().getStatus();
        assertThat(estado).isIn(401, 403);
    }

    @Test
    void laDemostracionNoSeRegistraFueraDeSuPerfil() {
        var rutas = requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPatternValues().stream()).toList();
        assertThat(rutas).noneMatch(ruta -> ruta.contains("demo-b01"));
    }

    @Test
    void unNombreConFormaDeUuidNoSustituyeLaIdentidadVerificadaPorA() throws Exception {
        // Este contexto no instala adaptadores ficticios: el puerto por defecto deniega.
        http.perform(get("/api/v1/mascotas/contexto")
                        .with(user("11111111-1111-4111-8111-111111111111")))
                .andExpect(status().isUnauthorized());
        http.perform(get("/api/v1/mascotas").with(user("usuario@example.test")))
                .andExpect(status().isUnauthorized());
    }
}
