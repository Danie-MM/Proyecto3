package cr.petmatchcr.mascotas.repository;

import cr.petmatchcr.mascotas.dto.CrearMascotaSolicitud;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MascotaRepositoryTest {
    @Autowired MascotaRepository mascotas;
    @Autowired JdbcTemplate jdbc;

    @Test
    void guardaElPerfilIndividualConLasEtiquetasActivasYRestringeSuConsulta() {
        UUID responsable = UUID.randomUUID();
        UUID primera = etiqueta(true);
        UUID segunda = etiqueta(true);
        UUID mascota = mascotas.crearIndividual(responsable, luna(List.of(primera, segunda)));

        assertThat(jdbc.queryForMap("""
                SELECT id, nombre, version, activa, disponibilidad FROM ret_id_mascota_pr(?, ?)
                """, mascota, responsable))
                .containsEntry("id", mascota).containsEntry("nombre", "Luna")
                .containsEntry("version", 0).containsEntry("activa", true)
                .containsEntry("disponibilidad", "NO_DISPONIBLE");
        assertThat(jdbc.queryForList("SELECT etiqueta_id FROM mascota_comportamiento WHERE mascota_id = ?",
                UUID.class, mascota)).containsExactlyInAnyOrder(primera, segunda);
        assertThat(jdbc.queryForList("SELECT id FROM ret_id_mascota_pr(?, ?)", mascota, UUID.randomUUID()))
                .isEmpty();
    }

    @Test
    void rechazaUnaEtiquetaInexistenteAunqueLaOtraEsteActiva() {
        var solicitud = luna(List.of(etiqueta(true), UUID.randomUUID()));
        assertThatThrownBy(() -> mascotas.crearIndividual(UUID.randomUUID(), solicitud))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaUnaEtiquetaDesactivadaAunqueLaOtraEsteActiva() {
        var solicitud = luna(List.of(etiqueta(true), etiqueta(false)));
        assertThatThrownBy(() -> mascotas.crearIndividual(UUID.randomUUID(), solicitud))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID etiqueta(boolean activa) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO etiqueta_comportamiento (id, nombre, activa) VALUES (?, ?, ?)",
                id, "Etiqueta de prueba", activa);
        return id;
    }

    private CrearMascotaSolicitud luna(List<UUID> etiquetas) {
        return new CrearMascotaSolicitud("Luna", "GATO", 8, "PEQUENO", "MEDIA", etiquetas);
    }
}
