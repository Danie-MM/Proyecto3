package cr.petmatchcr.mascotas.publicacion;

import cr.petmatchcr.mascotas.dto.MascotaPublicaRespuesta;
import cr.petmatchcr.mascotas.service.ErrorMascota;
import cr.petmatchcr.security.JwtAuthFilter;
import cr.petmatchcr.security.JwtService;
import cr.petmatchcr.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Ejecuta la cadena compartida con un visitante real anónimo; no requiere PostgreSQL. */
@WebMvcTest(controllers=PublicacionController.class, includeFilters=@ComponentScan.Filter(
        type=FilterType.REGEX, pattern="cr\\.petmatchcr\\.config\\.SeguridadHttp"))
@Import({JwtAuthFilter.class, JwtService.class, PublicacionSeguridadHttpTest.ServiciosWeb.class})
@ActiveProfiles("test")
class PublicacionSeguridadHttpTest {
    @TestConfiguration
    @EnableWebSecurity
    static class ServiciosWeb {}

    @Autowired MockMvc http;
    @MockitoBean PublicacionService servicio;
    @MockitoBean UserDetailsServiceImpl usuarios;
    UUID mascota = UUID.randomUUID();
    String ruta = "/api/v1/mascotas/" + mascota;

    @Test
    void visitanteRecibeSoloLasDosRespuestasPublicas() throws Exception {
        var perfil = new MascotaPublicaRespuesta(mascota,"Luna","PERRO",4,"MEDIANO","BAJA", List.of(),
                ruta+"/foto/publica",new MascotaPublicaRespuesta.Organizacion(UUID.randomUUID(),"Refugio"),"DISPONIBLE");
        byte[] png = FotografiaVerificadaTest.imagen("png");
        when(servicio.consultarPerfilPublico(mascota)).thenReturn(perfil);
        when(servicio.fotoPublica(mascota)).thenReturn(new PublicacionRepository.Foto(png,"image/png"));
        http.perform(get(ruta+"/publica")).andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Luna"));
        http.perform(get(ruta+"/foto/publica")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store")).andExpect(content().bytes(png));
        when(servicio.consultarPerfilPublico(mascota)).thenThrow(new ErrorMascota(404,"NO_ENCONTRADO","Oculta"));
        when(servicio.fotoPublica(mascota)).thenThrow(new ErrorMascota(404,"NO_ENCONTRADO","Oculta"));
        http.perform(get(ruta+"/publica")).andExpect(status().isNotFound());
        http.perform(get(ruta+"/foto/publica")).andExpect(status().isNotFound());
    }

    @Test
    void losVecinosYLasEscriturasSiguenProtegidos() throws Exception {
        for (String privada : new String[]{"/api/v1/mascotas",ruta,ruta+"/foto",ruta+"/publica/extra"}) {
            http.perform(get(privada)).andExpect(status().isUnauthorized());
        }
        http.perform(post(ruta+"/foto/retiro").with(csrf())).andExpect(status().isUnauthorized());
        http.perform(put(ruta+"/disponibilidad").with(csrf())).andExpect(status().isUnauthorized());
        http.perform(get(ruta+"/publica").header("Authorization","Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio,usuarios);
    }
}
