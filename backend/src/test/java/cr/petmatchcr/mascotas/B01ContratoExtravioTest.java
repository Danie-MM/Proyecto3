package cr.petmatchcr.mascotas;

import cr.petmatchcr.mascotas.api.AmbitosMascotas;
import cr.petmatchcr.mascotas.api.IdentidadMascotas;
import cr.petmatchcr.mascotas.api.MascotasParaExtravios;
import cr.petmatchcr.mascotas.dto.CrearMascotaSolicitud;
import cr.petmatchcr.mascotas.dto.DesactivarMascotaSolicitud;
import cr.petmatchcr.mascotas.service.ErrorMascota;
import cr.petmatchcr.mascotas.service.MascotaService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifica la frontera B→C sin convertir el contrato interno en un perfil público. */
@SpringBootTest
@ActiveProfiles("test")
@Import(B01ContratoExtravioTest.ConfiguracionAmbitos.class)
class B01ContratoExtravioTest {
    @Autowired MascotaService mascotas;
    @Autowired MascotasParaExtravios paraExtravios;
    @Autowired JdbcTemplate jdbc;
    @Value("${spring.flyway.default-schema}") String esquemaPruebas;
    @Autowired ObjectMapper json;
    @Autowired AmbitosDePrueba ambitos;
    @Autowired PlatformTransactionManager transacciones;

    private UUID responsable;
    private UUID representante;
    private UUID ajeno;
    private UUID organizacion;
    private UUID etiqueta;

    @BeforeEach
    void prepararSoloDatosDeEsteCaso() {
        assertThat(jdbc.queryForObject("SELECT current_schema()", String.class))
                .matches("pruebas_[a-z0-9_]+").isEqualTo(esquemaPruebas);
        responsable = UUID.randomUUID();
        representante = UUID.randomUUID();
        ajeno = UUID.randomUUID();
        organizacion = UUID.randomUUID();
        etiqueta = UUID.randomUUID();
        ambitos.porUsuario.put(responsable, Set.of(organizacion));
        ambitos.porUsuario.put(representante, Set.of(organizacion));
        jdbc.update("INSERT INTO etiqueta_comportamiento(id, nombre, activa) VALUES (?, ?, true)",
                etiqueta, "Etiqueta ficticia del contrato B-C");
    }

    @AfterEach
    void limpiarSolamenteNuestrosIdentificadores() {
        SecurityContextHolder.clearContext();
        if (responsable == null) return;
        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            jdbc.update("DELETE FROM mascota_desactivacion WHERE actor_usuario_id = ?", responsable);
            jdbc.update("DELETE FROM mascota_operacion_idempotente WHERE actor_usuario_id = ?", responsable);
            jdbc.update("DELETE FROM mascota_comportamiento WHERE mascota_id IN "
                    + "(SELECT id FROM mascota WHERE responsable_usuario_id = ?)", responsable);
            jdbc.update("DELETE FROM mascota WHERE responsable_usuario_id = ?", responsable);
            if (etiqueta != null) jdbc.update("DELETE FROM etiqueta_comportamiento WHERE id = ?", etiqueta);
        });
        ambitos.porUsuario.remove(responsable);
        ambitos.porUsuario.remove(representante);
        ambitos.identidadesRevocadas.clear();
    }

    @Test
    void elResponsableObtieneSoloLaIdentificacionMinimaParaC() throws Exception {
        UUID id = crear(null);
        conActor(responsable, () -> { paraExtravios.comprobarResponsable(id); return null; });
        var resumen = conActor(responsable, () -> paraExtravios.consultarResumenParaExtravio(id));
        assertThat(resumen.mascotaId()).isEqualTo(id);
        assertThat(resumen.nombre()).isEqualTo("Luna B-C");
        assertThat(resumen.especie()).isEqualTo("GATO");
        // Lista permitida: no responsable, organización, comportamientos, contactos ni clínica.
        Map<?, ?> salida = json.readValue(json.writeValueAsString(resumen), Map.class);
        assertThat(salida.keySet().stream().map(Object::toString).toList())
                .containsExactlyInAnyOrder("mascotaId", "nombre", "especie");
    }

    @Test
    void elResponsableDeOrganizacionNecesitaSuAmbitoVigente() throws Exception {
        UUID id = crear(organizacion);
        conActor(responsable, () -> { paraExtravios.comprobarResponsable(id); return null; });
        assertThat(conActor(responsable, () -> paraExtravios.consultarResumenParaExtravio(id)).mascotaId())
                .isEqualTo(id);
        ambitos.porUsuario.remove(responsable);
        comprobarNoEncontradaEnAmbasOperaciones(responsable, id);
    }

    @Test
    void otroRepresentantePuedeGestionarEnBPeroNoSuplantarAlResponsableEnC() throws Exception {
        UUID id = crear(organizacion);
        assertThat(conActor(representante, () -> mascotas.consultar(id)).id()).isEqualTo(id);
        comprobarNoEncontradaEnAmbasOperaciones(representante, id);
    }

    @Test
    void unUsuarioAjenoRecibeLoMismoParaUnPerfilExistenteQueParaUnoInexistente() throws Exception {
        UUID id = crear(null);
        comprobarNoEncontradaEnAmbasOperaciones(ajeno, id);
        comprobarNoEncontradaEnAmbasOperaciones(ajeno, UUID.randomUUID());
    }

    @Test
    void elContratoInternoTampocoRevelaElResumenSinSesion() throws Exception {
        UUID id = crear(null);
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> paraExtravios.comprobarResponsable(id))
                .isInstanceOfSatisfying(ErrorMascota.class, error -> assertThat(error.getEstado()).isEqualTo(401));
        assertThatThrownBy(() -> paraExtravios.consultarResumenParaExtravio(id))
                .isInstanceOfSatisfying(ErrorMascota.class, error -> assertThat(error.getEstado()).isEqualTo(401));
    }

    @Test
    void unaMascotaDesactivadaNoProduceUnResumenParaCrearUnExtravio() throws Exception {
        UUID id = crear(null);
        conActor(responsable, () -> mascotas.desactivar(id, UUID.randomUUID(),
                new DesactivarMascotaSolicitud("Perfil ficticio cerrado", 0)));
        assertThatThrownBy(() -> conActor(responsable, () -> paraExtravios.consultarResumenParaExtravio(id)))
                .isInstanceOfSatisfying(ErrorMascota.class, error -> {
                    assertThat(error.getEstado()).isEqualTo(409);
                    assertThat(error.getCodigo()).isEqualTo("CONFLICTO");
                });
    }

    @Test
    void elPuertoDeIdentidadPuedeRetirarUnaCuentaSinCambiarElSecurityContext() throws Exception {
        UUID id = crear(null);
        ambitos.identidadesRevocadas.add(responsable);
        assertThatThrownBy(() -> conActor(responsable, () -> paraExtravios.consultarResumenParaExtravio(id)))
                .isInstanceOfSatisfying(ErrorMascota.class, error -> assertThat(error.getEstado()).isEqualTo(401));
    }

    private UUID crear(UUID organizacionId) throws Exception {
        return conActor(responsable, () -> mascotas.crear(UUID.randomUUID(),
                new CrearMascotaSolicitud("Luna B-C", "GATO", 8, "PEQUENO", "MEDIA",
                        List.of(etiqueta), organizacionId))).id();
    }

    private void comprobarNoEncontradaEnAmbasOperaciones(UUID actor, UUID id) {
        assertThatThrownBy(() -> conActor(actor, () -> { paraExtravios.comprobarResponsable(id); return null; }))
                .isInstanceOfSatisfying(ErrorMascota.class, this::comprobar404);
        assertThatThrownBy(() -> conActor(actor, () -> paraExtravios.consultarResumenParaExtravio(id)))
                .isInstanceOfSatisfying(ErrorMascota.class, this::comprobar404);
    }

    private void comprobar404(ErrorMascota error) {
        assertThat(error.getEstado()).isEqualTo(404);
        assertThat(error.getCodigo()).isEqualTo("NO_ENCONTRADO");
    }

    private <T> T conActor(UUID actor, Callable<T> accion) throws Exception {
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new UsernamePasswordAuthenticationToken(actor.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        SecurityContextHolder.setContext(contexto);
        try {
            return accion.call();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    static class AmbitosDePrueba implements AmbitosMascotas, IdentidadMascotas {
        final Map<UUID, Set<UUID>> porUsuario = new ConcurrentHashMap<>();
        final Set<UUID> identidadesRevocadas = ConcurrentHashMap.newKeySet();

        // Este traductor solo existe en tests con actores controlados: A usa email/Long.
        @Override
        public Optional<UUID> usuarioActual() {
            var autenticacion = SecurityContextHolder.getContext().getAuthentication();
            if (autenticacion == null || !autenticacion.isAuthenticated()) return Optional.empty();
            try {
                UUID id = UUID.fromString(autenticacion.getName());
                return identidadesRevocadas.contains(id) ? Optional.empty() : Optional.of(id);
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
        }

        @Override
        public Set<UUID> organizacionesAdministrables(UUID usuarioId) {
            return porUsuario.getOrDefault(usuarioId, Set.of());
        }
    }

    @TestConfiguration
    static class ConfiguracionAmbitos {
        @Bean
        @Primary
        AmbitosDePrueba ambitosDePrueba() { return new AmbitosDePrueba(); }
    }
}
