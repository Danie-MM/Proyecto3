package cr.petmatchcr;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Fuera de local/test, la utilidad técnica no debe registrar rutas HTTP. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://db:5432/petmatchcr?currentSchema=pruebas",
    "spring.flyway.schemas=pruebas",
    "spring.flyway.default-schema=pruebas",
    "spring.jpa.properties.hibernate.default_schema=pruebas"
})
@AutoConfigureMockMvc
@ActiveProfiles("verificacion-seguridad")
class PerfilRestringidoIntegrationTest {
    @Autowired MockMvc http;
    @Autowired RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    void laUtilidadTecnicaNoExisteFueraDelEntornoLocal() throws Exception {
        boolean tieneRutaTecnica = requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
            .flatMap(info -> info.getPatternValues().stream())
            .anyMatch(ruta -> ruta.startsWith("/api/v1/pruebas-entorno"));
        assertThat(tieneRutaTecnica).isFalse();
        int estado = http.perform(get("/api/v1/pruebas-entorno/11111111-1111-4111-8111-111111111111"))
            .andReturn().getResponse().getStatus();
        assertThat(estado).isIn(401, 403);
    }
}
