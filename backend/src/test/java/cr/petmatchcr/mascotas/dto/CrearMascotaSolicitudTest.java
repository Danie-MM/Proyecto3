package cr.petmatchcr.mascotas.dto;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CrearMascotaSolicitudTest {
    private static final ValidatorFactory VALIDACION = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void cerrarValidacion() {
        VALIDACION.close();
    }

    private CrearMascotaSolicitud conNombre(String nombre) {
        return new CrearMascotaSolicitud(nombre, "GATO", 8, "PEQUENO", "MEDIA", List.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PERRO", "GATO", "CONEJO", "AVE"})
    void aceptaElFormatoDeCodigoLaExistenciaSeValidaEnLaBase(String especie) {
        var solicitud = new CrearMascotaSolicitud("Luna", especie, 8, "PEQUENO", "MEDIA", List.of());
        assertThat(VALIDACION.getValidator().validateProperty(solicitud, "especie")).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"gato", " GATO ", "1GATO", "CODIGO_QUE_SUPERA_TREINTA_CARACTERES"})
    void rechazaUnaEspecieAusenteOConFormatoInvalido(String especie) {
        var solicitud = new CrearMascotaSolicitud("Luna", especie, 8, "PEQUENO", "MEDIA", List.of());
        assertThat(VALIDACION.getValidator().validateProperty(solicitud, "especie")).isNotEmpty();
    }

    @ParameterizedTest
    @CsvSource({"PEQUENO, BAJA", "MEDIANO, MEDIA", "GRANDE, ALTA"})
    void aceptaLosTamanosYNivelesDeEnergiaDelCatalogo(String tamano, String energia) {
        var solicitud = new CrearMascotaSolicitud("Luna", "GATO", 8, tamano, energia, List.of());
        var validador = VALIDACION.getValidator();
        assertThat(validador.validateProperty(solicitud, "tamano")).isEmpty();
        assertThat(validador.validateProperty(solicitud, "energia")).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"MINI", "mediano"})
    void rechazaTamanoAusenteOFueraDelCatalogo(String tamano) {
        var solicitud = new CrearMascotaSolicitud("Luna", "GATO", 8, tamano, "MEDIA", List.of());
        assertThat(VALIDACION.getValidator().validateProperty(solicitud, "tamano")).isNotEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"MUY_ALTA", "media"})
    void rechazaEnergiaAusenteOFueraDelCatalogo(String energia) {
        var solicitud = new CrearMascotaSolicitud("Luna", "GATO", 8, "PEQUENO", energia, List.of());
        assertThat(VALIDACION.getValidator().validateProperty(solicitud, "energia")).isNotEmpty();
    }

    @Test
    void recortaSoloLosExtremos() {
        var solicitud = conNombre("  Luna María  ");
        assertThat(solicitud.nombre()).isEqualTo("Luna María");
        assertThat(VALIDACION.getValidator().validateProperty(solicitud, "nombre")).isEmpty();
    }

    @Test
    void aceptaUnaOMasEtiquetasDistintas() {
        UUID primera = UUID.randomUUID();
        UUID segunda = UUID.randomUUID();
        for (var etiquetas : List.of(List.of(primera), List.of(primera, segunda))) {
            var luna = new CrearMascotaSolicitud("Luna", "GATO", 8, "PEQUENO", "MEDIA", etiquetas);
            assertThat(VALIDACION.getValidator().validate(luna)).isEmpty();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    void exigeAlMenosUnaEtiqueta(List<UUID> etiquetas) {
        var luna = new CrearMascotaSolicitud("Luna", "GATO", 8, "PEQUENO", "MEDIA", etiquetas);
        assertThat(VALIDACION.getValidator().validate(luna)).hasSize(1);
    }

    @Test
    void rechazaEtiquetasRepetidas() {
        UUID etiqueta = UUID.randomUUID();
        var luna = new CrearMascotaSolicitud("Luna", "GATO", 8, "PEQUENO", "MEDIA", List.of(etiqueta, etiqueta));
        assertThat(VALIDACION.getValidator().validate(luna)).hasSize(1);
    }

    @Test
    void rechazaUnaEtiquetaSinIdentificadorAunqueHayaOtraValida() {
        var etiquetas = Arrays.asList(UUID.randomUUID(), null);
        var luna = new CrearMascotaSolicitud("Luna", "GATO", 8, "PEQUENO", "MEDIA", etiquetas);
        assertThat(VALIDACION.getValidator().validate(luna)).hasSize(1);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void exigeUnNombre(String nombre) {
        assertThat(VALIDACION.getValidator().validateProperty(conNombre(nombre), "nombre")).isNotEmpty();
    }

    @Test
    void cuentaCaracteresUnicodeDespuesDeRecortar() {
        String limite = "🐾".repeat(100);
        var validador = VALIDACION.getValidator();
        assertThat(validador.validateProperty(conNombre("  " + limite + " "), "nombre")).isEmpty();
        assertThat(validador.validateProperty(conNombre(limite + "a"), "nombre")).isNotEmpty();
    }
}
