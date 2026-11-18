package cr.petmatchcr.mascotas.publicacion;

import cr.petmatchcr.mascotas.api.AmbitosMascotas;
import cr.petmatchcr.mascotas.api.IdentidadMascotas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(PublicacionIntegrationTest.Actores.class)
class PublicacionIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc http;
    @Autowired Organizaciones organizaciones;
    @Autowired PublicacionService servicio;
    @MockitoSpyBean PublicacionRepository persistencia;
    UUID actor = UUID.randomUUID();
    UUID org = UUID.randomUUID();
    UUID mascota = UUID.randomUUID();
    String ruta = "/api/v1/mascotas/" + mascota;

    @BeforeEach
    void crearPerfilFicticio() {
        assertThat(jdbc.queryForObject("SELECT current_schema()", String.class)).matches("pruebas_[a-z0-9_]+");
        organizaciones.representantes.put(actor, org);
        organizaciones.activas.add(org);
        jdbc.update("""
                INSERT INTO mascota(id,nombre,especie,edad_estimada_meses,tamano,energia,responsable_usuario_id,organizacion_id)
                VALUES (?,'Luna','PERRO',4,'MEDIANO','BAJA',?,?)
                """, mascota, actor, org);
    }

    @AfterEach
    void limpiarPerfilDeEsteCaso() {
        assertThat(jdbc.queryForObject("SELECT current_schema()", String.class)).matches("pruebas_[a-z0-9_]+");
        jdbc.update("DELETE FROM operacion_foto_mascota WHERE mascota_id=?", mascota);
        jdbc.update("DELETE FROM archivo_mascota WHERE mascota_id=?", mascota);
        jdbc.update("DELETE FROM mascota WHERE id=?", mascota);
        organizaciones.activas.remove(org);
        organizaciones.representantes.remove(actor);
    }

    @Test
    void publicacionOcultamientoVigenciaYConflicto() throws Exception {
        disponibilidad(0,"DISPONIBLE").andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        assertThat(servicio.esPublicable(mascota)).isTrue();
        http.perform(get(ruta+"/publica"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Luna"))
                .andExpect(jsonPath("$.organizacion.id").value(org.toString()))
                .andExpect(jsonPath("$.responsableUsuarioId").doesNotExist())
                .andExpect(jsonPath("$.ubicacion").doesNotExist()).andExpect(jsonPath("$.expediente").doesNotExist());
        disponibilidad(0,"NO_DISPONIBLE").andExpect(status().isConflict());
        organizaciones.activas.remove(org);
        assertThat(servicio.esPublicable(mascota)).isFalse();
        http.perform(get(ruta+"/publica")).andExpect(status().isNotFound());
        organizaciones.activas.add(org);
        disponibilidad(1,"NO_DISPONIBLE").andExpect(status().isOk());
        assertThat(servicio.esPublicable(mascota)).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mascota WHERE id=?", Integer.class, mascota)).isEqualTo(1);
    }

    @Test
    void noPublicaParticularNiReabreAdoptada() throws Exception {
        jdbc.update("UPDATE mascota SET organizacion_id=NULL WHERE id=?",mascota);
        disponibilidad(0,"DISPONIBLE").andExpect(status().isConflict());
        disponibilidad(0,"ADOPTADA").andExpect(status().isBadRequest());
        jdbc.update("UPDATE mascota SET disponibilidad='ADOPTADA' WHERE id=?",mascota);
        disponibilidad(0,"NO_DISPONIBLE").andExpect(status().isConflict());
    }

    @Test
    void reemplazoInvalidoReenvioYRetirosCompruebanPermisosYVersion() throws Exception {
        byte[] png = FotografiaVerificadaTest.imagen("png");
        UUID clave = UUID.randomUUID();
        cargar(png,0,clave).andExpect(status().isCreated())
                .andExpect(header().string("Location",ruta+"/foto"))
                .andExpect(jsonPath("$.versionMascota").value(1));
        cargar(png,0,clave).andExpect(status().isCreated()).andExpect(jsonPath("$.versionMascota").value(1));
        cargar(png,1,clave).andExpect(status().isConflict());
        cargar(new byte[]{1,2,3},1,UUID.randomUUID()).andExpect(status().isUnsupportedMediaType());
        cargar(png,0,UUID.randomUUID()).andExpect(status().isConflict());
        http.perform(get(ruta).with(user(actor.toString()))).andExpect(jsonPath("$.fotoUrl").value(ruta+"/foto"));
        http.perform(get(ruta+"/foto").with(user(actor.toString()))).andExpect(status().isOk())
                .andExpect(content().bytes(png));
        http.perform(get(ruta+"/foto").with(user(UUID.randomUUID().toString()))).andExpect(status().isNotFound());
        http.perform(get(ruta+"/foto/publica")).andExpect(status().isNotFound());
        disponibilidad(1,"DISPONIBLE").andExpect(status().isOk());
        http.perform(get(ruta+"/foto/publica")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store")).andExpect(content().bytes(png));
        disponibilidad(2,"NO_DISPONIBLE").andExpect(status().isOk());
        http.perform(get(ruta+"/foto/publica")).andExpect(status().isNotFound());
        byte[] jpeg = FotografiaVerificadaTest.imagen("jpeg");
        cargar(jpeg,3,UUID.randomUUID()).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM archivo_mascota WHERE mascota_id=? AND activa",
                Integer.class,mascota)).isEqualTo(1);
        var retiro = UUID.randomUUID();
        for (int i=0; i<2; i++) {
            http.perform(post(ruta+"/foto/retiro").with(user(actor.toString())).with(csrf())
                .header("Idempotency-Key",retiro).contentType(MediaType.APPLICATION_JSON)
                .content("{\"versionEsperada\":4}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.versionMascota").value(5));
        }
        http.perform(get(ruta+"/foto").with(user(actor.toString()))).andExpect(status().isNotFound());
    }

    @Test
    void unFalloTrasEscribirRevierteLaFotoLaVersionYElRecibo() throws Exception {
        byte[] original = FotografiaVerificadaTest.imagen("png");
        cargar(original,0,UUID.randomUUID()).andExpect(status().isCreated());
        byte[] reemplazo = FotografiaVerificadaTest.imagen("jpeg");
        var clave = UUID.randomUUID();
        doAnswer(llamada -> {
            llamada.callRealMethod();
            throw new org.springframework.dao.DataAccessResourceFailureException("Fallo ficticio de almacenamiento");
        }).when(persistencia).reemplazar(any(),eq(actor),any());

        cargar(reemplazo,1,clave).andExpect(status().is5xxServerError());
        http.perform(get(ruta+"/foto").with(user(actor.toString())))
                .andExpect(status().isOk()).andExpect(content().bytes(original));
        assertThat(jdbc.queryForObject("SELECT version FROM mascota WHERE id=?", Integer.class, mascota)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM operacion_foto_mascota WHERE clave=? AND actor_id=?",
                Integer.class,clave,actor)).isZero();
        doCallRealMethod().when(persistencia).reemplazar(any(),eq(actor),any());
        cargar(reemplazo,1,clave).andExpect(status().isCreated()).andExpect(jsonPath("$.versionMascota").value(2));
    }

    private ResultActions disponibilidad(int version,String valor) throws Exception {
        return http.perform(put(ruta+"/disponibilidad").with(user(actor.toString())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(
                    "{\"versionEsperada\":"+version+",\"disponibilidad\":\""+valor+"\"}"));
    }

    private ResultActions cargar(byte[] bytes,int version,UUID clave) throws Exception {
        return http.perform(multipart(ruta+"/foto").file(new MockMultipartFile("archivo","foto.png","image/png",bytes))
                .param("versionEsperada",Integer.toString(version)).header("Idempotency-Key",clave)
                .with(user(actor.toString())).with(csrf()));
    }

    static class Organizaciones implements AmbitosMascotas {
        final java.util.Map<UUID,UUID> representantes = new ConcurrentHashMap<>();
        final Set<UUID> activas = ConcurrentHashMap.newKeySet();
        public Set<UUID> organizacionesAdministrables(UUID usuario) {
            var org = representantes.get(usuario);
            return org == null || !activas.contains(org) ? Set.of() : Set.of(org);
        }
        public Optional<String> nombreOrganizacionActiva(UUID org) {
            return activas.contains(org) ? Optional.of("Refugio de prueba") : Optional.empty();
        }
    }
    @TestConfiguration
    static class Actores {
        @Bean @Primary Organizaciones organizacionesB02() { return new Organizaciones(); }
        @Bean @Primary IdentidadMascotas identidadB02() {
            return () -> Optional.of(UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName()));
        }
    }
}
