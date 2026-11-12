package cr.petmatchcr.mascotas;

import cr.petmatchcr.mascotas.dto.CrearMascotaSolicitud;
import cr.petmatchcr.mascotas.dto.DesactivarMascotaSolicitud;
import cr.petmatchcr.mascotas.dto.EditarMascotaSolicitud;
import cr.petmatchcr.mascotas.dto.MascotaPrivadaRespuesta;
import cr.petmatchcr.mascotas.repository.MascotaRepository;
import java.util.Set;
import java.sql.SQLException;
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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba las carreras reales que un test con rollback en un solo hilo no reproduce.
 * Cada hilo llama al repositorio con su propia transacción PostgreSQL.
 * Estos casos verifican persistencia/concurrencia; los casos HTTP/CSRF viven en B01MascotasIntegrationTest.
 */
@SpringBootTest
@ActiveProfiles("test")
class B01ConcurrenciaTest {
    @Autowired MascotaRepository repositorio;
    @Autowired JdbcTemplate jdbc;
    @Value("${spring.flyway.default-schema}") String esquemaPruebas;
    @Autowired PlatformTransactionManager transacciones;

    private UUID actor;
    private final List<UUID> etiquetas = new ArrayList<>();

    @BeforeEach
    void prepararDatosAislados() {
        assertThat(jdbc.queryForObject("SELECT current_schema()", String.class))
                .matches("pruebas_[a-z0-9_]+").isEqualTo(esquemaPruebas);
        actor = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO etiqueta_comportamiento(id, nombre, activa) VALUES (?, ?, true)",
                    id, "Etiqueta ficticia concurrencia " + i);
            etiquetas.add(id);
        }
    }

    @AfterEach
    void retirarSolamenteLosDatosDeEsteCaso() {
        if (actor == null) return;
        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            jdbc.update("DELETE FROM mascota_desactivacion WHERE actor_usuario_id = ?", actor);
            jdbc.update("DELETE FROM mascota_operacion_idempotente WHERE actor_usuario_id = ?", actor);
            jdbc.update("DELETE FROM mascota_comportamiento WHERE mascota_id IN "
                    + "(SELECT id FROM mascota WHERE responsable_usuario_id = ?)", actor);
            jdbc.update("DELETE FROM mascota WHERE responsable_usuario_id = ?", actor);
            for (UUID etiqueta : etiquetas) {
                jdbc.update("DELETE FROM etiqueta_comportamiento WHERE id = ?", etiqueta);
            }
        });
    }

    @Test
    void dosAltasSimultaneasConLaMismaClaveCreanUnSoloPerfil() throws Exception {
        UUID clave = UUID.randomUUID();
        var resultados = simultaneamente(
                () -> crear(clave, alta()),
                () -> crear(clave, alta()));

        assertThat(resultados).allMatch(resultado -> resultado.estadoSql() == null);
        UUID id = resultados.getFirst().perfil().id();
        assertThat(resultados.getLast().perfil().id()).isEqualTo(id);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mascota WHERE responsable_usuario_id = ?",
                Integer.class, actor)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mascota_operacion_idempotente "
                + "WHERE actor_usuario_id = ? AND operacion = 'MASCOTA_CREAR' AND clave = ?",
                Integer.class, actor, clave)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT etiqueta_id FROM mascota_comportamiento WHERE mascota_id = ?",
                UUID.class, id)).containsExactly(etiquetas.getFirst());
    }

    @Test
    void dosEdicionesConLaMismaVersionNoMezclanNiSobrescribenLosDatos() throws Exception {
        var creada = crear(UUID.randomUUID(), alta());
        var cambioA = new EditarMascotaSolicitud("Luna A", "GATO", 9, "MEDIANO", "ALTA",
                List.of(etiquetas.get(1)), 0);
        var cambioB = new EditarMascotaSolicitud("Luna B", "GATO", 10, "GRANDE", "BAJA",
                List.of(etiquetas.get(2)), 0);
        var resultados = simultaneamente(
                () -> editar(creada.id(), cambioA),
                () -> editar(creada.id(), cambioB));

        assertThat(resultados.stream().filter(r -> r.estadoSql() == null).toList()).hasSize(1);
        assertThat(resultados.stream().map(Resultado::estadoSql).filter(estado -> estado != null).toList())
                .containsExactly("P0409");
        var ganador = resultados.stream().filter(r -> r.estadoSql() == null).findFirst().orElseThrow().perfil();
        var guardada = consultar(creada.id());
        assertThat(guardada).isEqualTo(ganador);
        assertThat(guardada.version()).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT etiqueta_id FROM mascota_comportamiento WHERE mascota_id = ?",
                UUID.class, creada.id())).containsExactlyElementsOf(ganador.comportamientoIds());
        // La respuesta debe corresponder a un cambio completo, no una mezcla de ambos.
        if (ganador.nombre().equals("Luna A")) {
            assertThat(ganador.edadEstimadaMeses()).isEqualTo(9);
            assertThat(ganador.tamano()).isEqualTo("MEDIANO");
            assertThat(ganador.energia()).isEqualTo("ALTA");
            assertThat(ganador.comportamientoIds()).containsExactly(etiquetas.get(1));
        } else {
            assertThat(ganador.nombre()).isEqualTo("Luna B");
            assertThat(ganador.edadEstimadaMeses()).isEqualTo(10);
            assertThat(ganador.tamano()).isEqualTo("GRANDE");
            assertThat(ganador.energia()).isEqualTo("BAJA");
            assertThat(ganador.comportamientoIds()).containsExactly(etiquetas.get(2));
        }
    }

    @Test
    void dosDesactivacionesConLaMismaClaveRegistranUnaSolaBaja() throws Exception {
        var creada = crear(UUID.randomUUID(), alta());
        UUID clave = UUID.randomUUID();
        var solicitud = new DesactivarMascotaSolicitud("Perfil ficticio duplicado", 0);
        var resultados = simultaneamente(
                () -> desactivar(creada.id(), clave, solicitud),
                () -> desactivar(creada.id(), clave, solicitud));

        assertThat(resultados).allMatch(r -> r.estadoSql() == null);
        assertThat(resultados).allMatch(r -> !r.perfil().activa() && r.perfil().version() == 1);
        assertThat(resultados.getFirst().perfil()).isEqualTo(resultados.getLast().perfil());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mascota_desactivacion WHERE mascota_id = ?",
                Integer.class, creada.id())).isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT actor_usuario_id, motivo, version_anterior, version_nueva "
                + "FROM mascota_desactivacion WHERE mascota_id = ?", creada.id()))
                .containsEntry("actor_usuario_id", actor).containsEntry("motivo", solicitud.motivo())
                .containsEntry("version_anterior", 0).containsEntry("version_nueva", 1);
    }

    private CrearMascotaSolicitud alta() {
        return new CrearMascotaSolicitud("Luna concurrente", "GATO", 8, "PEQUENO", "MEDIA",
                List.of(etiquetas.getFirst()));
    }

    private record Resultado(MascotaPrivadaRespuesta perfil, String estadoSql) {}

    private List<Resultado> simultaneamente(Callable<MascotaPrivadaRespuesta> primera,
                                           Callable<MascotaPrivadaRespuesta> segunda) throws Exception {
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch comenzar = new CountDownLatch(1);
        var ejecutor = Executors.newFixedThreadPool(2);
        try {
            var a = ejecutor.submit(() -> ejecutar(preparados, comenzar, primera));
            var b = ejecutor.submit(() -> ejecutar(preparados, comenzar, segunda));
            assertThat(preparados.await(5, TimeUnit.SECONDS)).isTrue();
            comenzar.countDown();
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        } finally {
            comenzar.countDown();
            ejecutor.shutdownNow();
            ejecutor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private Resultado ejecutar(CountDownLatch preparados, CountDownLatch comenzar,
                               Callable<MascotaPrivadaRespuesta> accion) throws Exception {
        preparados.countDown();
        if (!comenzar.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("No comenzaron ambos hilos.");
        try {
            return new Resultado(accion.call(), null);
        } catch (DataAccessException error) {
            if (error.getMostSpecificCause() instanceof SQLException sql) {
                return new Resultado(null, sql.getSQLState());
            }
            throw error;
        }
    }

    private MascotaPrivadaRespuesta crear(UUID clave, CrearMascotaSolicitud solicitud) {
        return MascotaPrivadaRespuesta.desde(repositorio.crear(actor, Set.of(), clave, solicitud));
    }

    private MascotaPrivadaRespuesta editar(UUID id, EditarMascotaSolicitud solicitud) {
        return MascotaPrivadaRespuesta.desde(repositorio.editar(id, actor, Set.of(), solicitud));
    }

    private MascotaPrivadaRespuesta consultar(UUID id) {
        return MascotaPrivadaRespuesta.desde(repositorio.consultar(id, actor, Set.of()).orElseThrow());
    }

    private MascotaPrivadaRespuesta desactivar(UUID id, UUID clave, DesactivarMascotaSolicitud solicitud) {
        return MascotaPrivadaRespuesta.desde(repositorio.desactivar(id, actor, Set.of(), clave, solicitud));
    }
}
