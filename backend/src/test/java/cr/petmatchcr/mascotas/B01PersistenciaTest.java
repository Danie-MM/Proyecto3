package cr.petmatchcr.mascotas;

import cr.petmatchcr.mascotas.dto.CrearMascotaSolicitud;
import cr.petmatchcr.mascotas.dto.DesactivarMascotaSolicitud;
import cr.petmatchcr.mascotas.dto.EditarMascotaSolicitud;
import cr.petmatchcr.mascotas.dto.MascotaPrivadaRespuesta;
import cr.petmatchcr.mascotas.repository.CatalogosMascotaRepository;
import cr.petmatchcr.mascotas.repository.MascotaRepository;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprueba el contrato de PostgreSQL mediante el repositorio real.
 * No usa una transacción envolvente: después de cada fallo consultamos otra transacción
 * para comprobar que no quedaron cambios parciales ni claves de reintento bloqueadas.
 * Los datos se crean exclusivamente en pruebas y se retiran por sus propios UUID.
 */
@SpringBootTest
@ActiveProfiles("test")
class B01PersistenciaTest {
    @Autowired MascotaRepository mascotas;
    @Autowired CatalogosMascotaRepository catalogos;
    @Autowired JdbcTemplate jdbc;
    @Value("${spring.flyway.default-schema}") String esquemaPruebas;
    @Autowired PlatformTransactionManager transacciones;

    private UUID actor;
    private String especie;
    private final List<UUID> etiquetas = new ArrayList<>();

    @BeforeEach
    void prepararCatalogoFicticio() {
        assertThat(jdbc.queryForObject("SELECT current_schema()", String.class))
                .matches("pruebas_[a-z0-9_]+").isEqualTo(esquemaPruebas);
        actor = UUID.randomUUID();
        especie = "CONEJO_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        jdbc.update("INSERT INTO especie(codigo, nombre, activa) VALUES (?, ?, true)",
                especie, "Conejo ficticio para prueba");
        for (int i = 0; i < 2; i++) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO etiqueta_comportamiento(id, nombre, activa) VALUES (?, ?, true)",
                    id, "Comportamiento ficticio " + i);
            etiquetas.add(id);
        }
    }

    @AfterEach
    void retirarSoloDatosDeEsteCaso() {
        if (actor == null) return;
        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            jdbc.update("DELETE FROM mascota_desactivacion WHERE actor_usuario_id = ?", actor);
            jdbc.update("DELETE FROM mascota_operacion_idempotente WHERE actor_usuario_id = ?", actor);
            jdbc.update("DELETE FROM mascota_comportamiento WHERE mascota_id IN "
                    + "(SELECT id FROM mascota WHERE responsable_usuario_id = ?)", actor);
            jdbc.update("DELETE FROM mascota WHERE responsable_usuario_id = ?", actor);
            for (UUID etiqueta : etiquetas) jdbc.update("DELETE FROM etiqueta_comportamiento WHERE id = ?", etiqueta);
            jdbc.update("DELETE FROM especie WHERE codigo = ?", especie);
        });
    }

    @Test
    void guardaUnaEspecieNuevaSinCambiarJavaYUsaLaConsultaManualDeV5() {
        assertThat(catalogos.existeEspecieActiva(especie)).isTrue();
        assertThat(catalogos.existeEspecieActiva("INEXISTENTE")).isFalse();
        assertThat(catalogos.listarEspecies()).anyMatch(e -> especie.equals(e.getCodigo()));
        var creada = mascotas.crear(actor, Set.of(), UUID.randomUUID(), alta(etiquetas));
        var leida = mascotas.consultar(creada.getId(), actor, Set.of()).orElseThrow();
        assertThat(leida.getEspecie().getCodigo()).isEqualTo(especie);
        assertThat(leida.getEspecie().getNombre()).isEqualTo("Conejo ficticio para prueba");
        assertThat(leida.getVersion()).isZero();
        assertThat(leida.getDisponibilidad()).isEqualTo("NO_DISPONIBLE");
        assertThat(leida.getComportamientoIds()).containsExactlyInAnyOrderElementsOf(etiquetas);
    }

    @Test
    void especieRetiradaImpideAltasPeroConservaElPerfilExistente() {
        var creada = mascotas.crear(actor, Set.of(), UUID.randomUUID(), alta(etiquetas));
        jdbc.update("UPDATE especie SET activa = false WHERE codigo = ?", especie);
        assertThat(catalogos.existeEspecieActiva(especie)).isFalse();
        fallaCon("P0400", () -> mascotas.crear(actor, Set.of(), UUID.randomUUID(), alta(etiquetas)));
        var editada = mascotas.editar(creada.getId(), actor, Set.of(), edicion("Luna mayor", etiquetas, 0));
        assertThat(editada.getNombre()).isEqualTo("Luna mayor");
        assertThat(editada.getEspecie().isActiva()).isFalse();
        assertThat(mascotas.contar(actor, Set.of(), null, null)).isEqualTo(1);
    }

    @Test
    void altaFallidaNoDejaMascotaNiClaveYPermiteCorregirElMismoIntento() {
        UUID clave = UUID.randomUUID();
        jdbc.update("UPDATE etiqueta_comportamiento SET activa = false WHERE id = ?", etiquetas.getLast());
        fallaCon("P0400", () -> mascotas.crear(actor, Set.of(), clave, alta(etiquetas)));
        assertThat(mascotas.contar(actor, Set.of(), null, null)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mascota_operacion_idempotente WHERE actor_usuario_id = ?",
                Integer.class, actor)).isZero();
        var corregida = mascotas.crear(actor, Set.of(), clave, alta(List.of(etiquetas.getFirst())));
        assertThat(corregida.getComportamientoIds()).containsExactly(etiquetas.getFirst());
    }

    @Test
    void edicionFallidaNoCambiaNombreVersionNiRelaciones() {
        var creada = mascotas.crear(actor, Set.of(), UUID.randomUUID(), alta(etiquetas));
        var antes = MascotaPrivadaRespuesta.desde(creada);
        fallaCon("P0400", () -> mascotas.editar(creada.getId(), actor, Set.of(),
                edicion("No debe guardarse", List.of(etiquetas.getFirst(), UUID.randomUUID()), 0)));
        var despues = mascotas.consultar(creada.getId(), actor, Set.of()).orElseThrow();
        assertThat(MascotaPrivadaRespuesta.desde(despues)).isEqualTo(antes);
    }

    @Test
    void puedeConservarYDespuesQuitarUnaEtiquetaRetirada() {
        var creada = mascotas.crear(actor, Set.of(), UUID.randomUUID(), alta(etiquetas));
        jdbc.update("UPDATE etiqueta_comportamiento SET activa = false WHERE id = ?", etiquetas.getLast());
        var conserva = mascotas.editar(creada.getId(), actor, Set.of(), edicion("Luna", etiquetas, 0));
        assertThat(conserva.getComportamientoIds()).containsExactlyInAnyOrderElementsOf(etiquetas);
        var quita = mascotas.editar(creada.getId(), actor, Set.of(),
                edicion("Luna", List.of(etiquetas.getFirst()), 1));
        assertThat(quita.getComportamientoIds()).containsExactly(etiquetas.getFirst());
        fallaCon("P0400", () -> mascotas.editar(creada.getId(), actor, Set.of(), edicion("Luna", etiquetas, 2)));
    }

    @Test
    void reintentoNormalizaOrdenYRecorteSinRevalidarCatalogoYaRetirado() {
        UUID clave = UUID.randomUUID();
        var creada = mascotas.crear(actor, Set.of(), clave, alta(etiquetas));
        jdbc.update("UPDATE especie SET activa = false WHERE codigo = ?", especie);
        jdbc.update("UPDATE etiqueta_comportamiento SET activa = false WHERE id = ?", etiquetas.getLast());
        var repetida = mascotas.crear(actor, Set.of(), clave, new CrearMascotaSolicitud(
                "  Luna  ", especie, 8, "PEQUENO", "MEDIA", etiquetas.reversed()));
        assertThat(repetida.getId()).isEqualTo(creada.getId());
        assertThat(mascotas.contar(actor, Set.of(), null, null)).isEqualTo(1);
        fallaCon("P0410", () -> mascotas.crear(actor, Set.of(), clave,
                new CrearMascotaSolicitud("Otro nombre", especie, 8, "PEQUENO", "MEDIA", etiquetas)));
    }

    @Test
    void perderRepresentacionQuitaAccesoInclusoAlCreador() {
        UUID organizacion = UUID.randomUUID();
        UUID clave = UUID.randomUUID();
        var solicitud = new CrearMascotaSolicitud("Luna", especie, 8, "PEQUENO", "MEDIA", etiquetas, organizacion);
        var creada = mascotas.crear(actor, Set.of(organizacion), clave, solicitud);
        assertThat(mascotas.consultar(creada.getId(), actor, Set.of())).isEmpty();
        assertThat(mascotas.listar(actor, Set.of(), null, null, 0, 20)).isEmpty();
        assertThat(mascotas.contar(actor, Set.of(), null, null)).isZero();
        fallaCon("P0404", () -> mascotas.editar(creada.getId(), actor, Set.of(), edicion("Luna", etiquetas, 0)));
        fallaCon("P0404", () -> mascotas.desactivar(creada.getId(), actor, Set.of(), UUID.randomUUID(),
                new DesactivarMascotaSolicitud("No autorizado", 0)));
        fallaCon("P0404", () -> mascotas.crear(actor, Set.of(), clave, solicitud));
        assertThat(mascotas.consultar(creada.getId(), actor, Set.of(organizacion))).isPresent();
    }

    @Test
    void bajaConservaPerfilYRelacionesYRegistraActorFechaMotivoVersion() {
        var creada = mascotas.crear(actor, Set.of(), UUID.randomUUID(), alta(etiquetas));
        fallaCon("P0400", () -> mascotas.desactivar(creada.getId(), actor, Set.of(), UUID.randomUUID(),
                new DesactivarMascotaSolicitud("x".repeat(501), 0)));
        UUID clave = UUID.randomUUID();
        var solicitud = new DesactivarMascotaSolicitud("  Registro duplicado  ", 0);
        var baja = mascotas.desactivar(creada.getId(), actor, Set.of(), clave, solicitud);
        assertThat(baja.getId()).isEqualTo(creada.getId());
        assertThat(baja.isActiva()).isFalse();
        assertThat(baja.getVersion()).isEqualTo(1);
        assertThat(baja.getComportamientoIds()).containsExactlyInAnyOrderElementsOf(etiquetas);
        mascotas.desactivar(creada.getId(), actor, Set.of(), clave, solicitud);
        var auditoria = jdbc.queryForMap("SELECT actor_usuario_id, fecha, motivo, version_anterior, version_nueva "
                + "FROM mascota_desactivacion WHERE mascota_id = ?", creada.getId());
        assertThat(auditoria).containsEntry("actor_usuario_id", actor).containsEntry("motivo", "Registro duplicado")
                .containsEntry("version_anterior", 0).containsEntry("version_nueva", 1);
        assertThat(auditoria.get("fecha")).isNotNull();
        fallaCon("P0411", () -> mascotas.editar(creada.getId(), actor, Set.of(), edicion("Luna", etiquetas, 1)));
    }

    private CrearMascotaSolicitud alta(List<UUID> seleccion) {
        return new CrearMascotaSolicitud("Luna", especie, 8, "PEQUENO", "MEDIA", seleccion);
    }

    private EditarMascotaSolicitud edicion(String nombre, List<UUID> seleccion, int version) {
        return new EditarMascotaSolicitud(nombre, especie, 9, "MEDIANO", "MEDIA", seleccion, version);
    }

    private void fallaCon(String estadoSql, Runnable accion) {
        assertThatThrownBy(accion::run).isInstanceOf(DataAccessException.class).satisfies(error -> {
            Throwable causa = ((DataAccessException) error).getMostSpecificCause();
            assertThat(causa).isInstanceOf(SQLException.class);
            assertThat(((SQLException) causa).getSQLState()).isEqualTo(estadoSql);
        });
    }
}
