package cr.petmatchcr.extravios;

import cr.petmatchcr.extravios.dto.ActualizarPreferenciaUbicacionDTO;
import cr.petmatchcr.extravios.dto.GuardarPreferenciaUbicacionDTO;
import cr.petmatchcr.extravios.dto.PreferenciaUbicacionDTO;
import cr.petmatchcr.extravios.service.ErrorUbicacion;
import cr.petmatchcr.extravios.service.PreferenciaUbicacionService;
import cr.petmatchcr.extravios.service.UsuarioActual;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Prueba persistencia real y carreras; el actor ficticio se limita a esta clase de pruebas. */
@SpringBootTest
@ActiveProfiles("test")
class C01UbicacionIntegrationTest {
    @Autowired PreferenciaUbicacionService servicio;
    @Autowired JdbcTemplate jdbc;
    @Value("${spring.flyway.default-schema}") String esquemaPruebas;
    @MockitoBean UsuarioActual usuarioActual;

    private UUID actor;
    private final List<UUID> actores = new ArrayList<>();

    @BeforeEach
    void prepararCuentaAislada() {
        assertThat(jdbc.queryForObject("SELECT current_schema()", String.class))
                .matches("pruebas_[a-z0-9_]+").isEqualTo(esquemaPruebas);
        actor = UUID.randomUUID();
        actores.add(actor);
        when(usuarioActual.obtenerUsuarioId()).thenReturn(actor);
    }

    @AfterEach
    void retirarSoloDatosDelCaso() {
        for (UUID usuario : actores) {
            jdbc.update("DELETE FROM preferencia_ubicacion WHERE usuario_id = ?", usuario);
            jdbc.update("DELETE FROM ubicacion_intento_creacion WHERE usuario_id = ?", usuario);
        }
    }

    @Test
    void ausenciaYCoordenadasOmitidasSonEstadosValidos() {
        var ausente = servicio.consultar();
        assertThat(ausente.configurada()).isFalse();
        assertThat(ausente.version()).isNull();
        var creada = servicio.crear(new GuardarPreferenciaUbicacionDTO(null, null, null, false), UUID.randomUUID());
        assertThat(creada.configurada()).isTrue();
        assertThat(creada.version()).isZero();
        assertThat(servicio.consultar()).isEqualTo(creada);
        assertThat(creada.latitud()).isNull();
        assertThat(creada.longitud()).isNull();
        assertThat(creada.consentimientoEn()).isNull();
    }

    @Test
    void validaParRangosAceptacionYPrecisionSinAlterarLoGuardado() {
        var original = servicio.crear(ubicacion(), UUID.randomUUID());
        var invalidas = List.of(
                new ActualizarPreferenciaUbicacionDTO(BigDecimal.ZERO, null, null, false, 0),
                new ActualizarPreferenciaUbicacionDTO(null, BigDecimal.ZERO, null, false, 0),
                new ActualizarPreferenciaUbicacionDTO(new BigDecimal("90.1"), BigDecimal.ZERO, null, false, 0),
                new ActualizarPreferenciaUbicacionDTO(BigDecimal.ZERO, new BigDecimal("-180.1"), null, false, 0),
                new ActualizarPreferenciaUbicacionDTO(new BigDecimal("9.1234567"), BigDecimal.ZERO, null, false, 0),
                new ActualizarPreferenciaUbicacionDTO(null, null, null, null, 0));
        for (var invalida : invalidas) {
            assertThatThrownBy(() -> servicio.actualizar(invalida)).isInstanceOfSatisfying(ErrorUbicacion.class,
                    error -> assertThat(error.getEstado()).isEqualTo(400));
        }
        assertThat(servicio.consultar()).isEqualTo(original);
    }

    @Test
    void versionAnteriorNoSobrescribeYRetirarAvisosConservaFechaDelCambio() {
        var original = servicio.crear(ubicacion(), UUID.randomUUID());
        assertThat(original.consentimientoEn()).isNotNull();
        var actualizada = servicio.actualizar(new ActualizarPreferenciaUbicacionDTO(null, null, null, false, 0));
        assertThat(actualizada.version()).isEqualTo(1);
        assertThat(actualizada.consentimientoEn()).isNotNull().isAfterOrEqualTo(original.consentimientoEn());
        assertThatThrownBy(() -> servicio.actualizar(cambio("Viejo", 0)))
                .isInstanceOfSatisfying(ErrorUbicacion.class,
                        error -> assertThat(error.getCodigo()).isEqualTo("VERSION_DESACTUALIZADA"));
        assertThatThrownBy(() -> servicio.actualizar(cambio("Sin versión", null)))
                .isInstanceOfSatisfying(ErrorUbicacion.class,
                        error -> assertThat(error.getEstado()).isEqualTo(400));
        assertThat(servicio.consultar()).isEqualTo(actualizada);
    }

    @Test
    void otraCuentaNoConsultaNiModificaLaPreferenciaAjena() {
        var original = servicio.crear(ubicacion(), UUID.randomUUID());
        UUID otro = UUID.randomUUID();
        actores.add(otro);
        when(usuarioActual.obtenerUsuarioId()).thenReturn(otro);
        assertThat(servicio.consultar().configurada()).isFalse();
        assertThatThrownBy(() -> servicio.actualizar(cambio("Ajena", 0)))
                .isInstanceOfSatisfying(ErrorUbicacion.class,
                        error -> assertThat(error.getEstado()).isEqualTo(404));
        when(usuarioActual.obtenerUsuarioId()).thenReturn(actor);
        assertThat(servicio.consultar()).isEqualTo(original);
    }

    @Test
    void reenvioConservaLaRespuestaOriginalYRechazaOtraHuella() {
        UUID clave = UUID.randomUUID();
        var original = servicio.crear(ubicacion(), clave);
        assertThat(servicio.crear(ubicacion(), clave)).isEqualTo(original);
        servicio.actualizar(cambio("Modificada después", 0));
        assertThat(servicio.crear(ubicacion(), clave)).isEqualTo(original);
        assertThatThrownBy(() -> servicio.crear(new GuardarPreferenciaUbicacionDTO(null, null, null, false), clave))
                .isInstanceOfSatisfying(ErrorUbicacion.class,
                        error -> assertThat(error.getCodigo()).isEqualTo("CLAVE_REUTILIZADA"));
        assertThat(cantidadPreferencias()).isEqualTo(1);
        assertThat(servicio.consultar().referencia()).isEqualTo("Modificada después");
    }

    @Test
    void reintentarAltaDespuesDeEliminarNoRecreaUbicacion() {
        UUID clave = UUID.randomUUID();
        servicio.crear(ubicacion(), clave);
        servicio.eliminar(0);
        assertThatThrownBy(() -> servicio.crear(ubicacion(), clave))
                .isInstanceOfSatisfying(ErrorUbicacion.class,
                        error -> assertThat(error.getEstado()).isEqualTo(409));
        assertThat(servicio.consultar().configurada()).isFalse();
        assertThat(cantidadPreferencias()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ubicacion_intento_creacion WHERE usuario_id = ?",
                Integer.class, actor)).isEqualTo(1);
        // Una intención nueva sí puede volver a configurar voluntariamente la ubicación.
        assertThat(servicio.crear(ubicacion(), UUID.randomUUID()).configurada()).isTrue();
    }

    @Test
    void eliminarConVersionObsoletaConservaLaUbicacion() {
        servicio.crear(ubicacion(), UUID.randomUUID());
        var vigente = servicio.actualizar(cambio("Vigente", 0));
        assertThatThrownBy(() -> servicio.eliminar(0)).isInstanceOfSatisfying(ErrorUbicacion.class,
                error -> assertThat(error.getCodigo()).isEqualTo("VERSION_DESACTUALIZADA"));
        assertThat(servicio.consultar()).isEqualTo(vigente);
    }

    @Test
    void dosAltasConLaMismaClaveCreanUnaPreferencia() throws Exception {
        UUID clave = UUID.randomUUID();
        var resultados = simultaneamente(() -> servicio.crear(ubicacion(), clave),
                () -> servicio.crear(ubicacion(), clave));
        assertThat(resultados).allMatch(resultado -> resultado.error() == null);
        assertThat(resultados.getFirst().respuesta()).isEqualTo(resultados.getLast().respuesta());
        assertThat(cantidadPreferencias()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ubicacion_intento_creacion WHERE usuario_id = ?",
                Integer.class, actor)).isEqualTo(1);
    }

    @Test
    void dosEdicionesDeLaMismaVersionTienenUnSoloGanador() throws Exception {
        servicio.crear(ubicacion(), UUID.randomUUID());
        var resultados = simultaneamente(() -> servicio.actualizar(cambio("Cambio A", 0)),
                () -> servicio.actualizar(cambio("Cambio B", 0)));
        assertThat(resultados.stream().filter(r -> r.error() == null).toList()).hasSize(1);
        var perdedor = resultados.stream().filter(r -> r.error() != null).findFirst().orElseThrow().error();
        assertThat(perdedor instanceof OptimisticLockingFailureException
                || perdedor instanceof ErrorUbicacion error && error.getEstado() == 409).isTrue();
        var ganadora = resultados.stream().filter(r -> r.error() == null).findFirst().orElseThrow().respuesta();
        assertThat(servicio.consultar()).isEqualTo(ganadora);
        assertThat(ganadora.version()).isEqualTo(1);
    }

    private int cantidadPreferencias() {
        return jdbc.queryForObject("SELECT count(*) FROM preferencia_ubicacion WHERE usuario_id = ?",
                Integer.class, actor);
    }

    private GuardarPreferenciaUbicacionDTO ubicacion() {
        return new GuardarPreferenciaUbicacionDTO(new BigDecimal("9.93"), new BigDecimal("-84.08"),
                "Punto ficticio", true);
    }

    private ActualizarPreferenciaUbicacionDTO cambio(String referencia, Integer version) {
        return new ActualizarPreferenciaUbicacionDTO(new BigDecimal("9.94"), new BigDecimal("-84.07"),
                referencia, true, version);
    }

    private record Resultado(PreferenciaUbicacionDTO respuesta, RuntimeException error) {}

    private List<Resultado> simultaneamente(Callable<PreferenciaUbicacionDTO> primera,
                                           Callable<PreferenciaUbicacionDTO> segunda) throws Exception {
        var preparados = new CountDownLatch(2);
        var comenzar = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var uno = ejecutor.submit(() -> ejecutar(preparados, comenzar, primera));
            var dos = ejecutor.submit(() -> ejecutar(preparados, comenzar, segunda));
            assertThat(preparados.await(5, TimeUnit.SECONDS)).isTrue();
            comenzar.countDown();
            return List.of(uno.get(30, TimeUnit.SECONDS), dos.get(30, TimeUnit.SECONDS));
        } finally {
            comenzar.countDown();
        }
    }

    private Resultado ejecutar(CountDownLatch preparados, CountDownLatch comenzar,
                               Callable<PreferenciaUbicacionDTO> accion) throws Exception {
        preparados.countDown();
        if (!comenzar.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Los hilos no comenzaron.");
        try {
            return new Resultado(accion.call(), null);
        } catch (RuntimeException error) {
            return new Resultado(null, error);
        }
    }
}
