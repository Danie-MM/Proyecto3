package cr.petmatchcr.mascotas;

import cr.petmatchcr.mascotas.api.AmbitosMascotas;
import cr.petmatchcr.mascotas.api.IdentidadMascotas;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorre HTTP, seguridad, servicio y PostgreSQL juntos. Cada petición conserva la
 * transacción real del servicio, de modo que también se comprueba su rollback.
 * Al terminar se limpian únicamente los UUID ficticios creados por cada caso en pruebas.
 * El adaptador de ámbitos simula únicamente el contrato interno que debe aportar A.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(B01MascotasIntegrationTest.ConfiguracionAmbitos.class)
class B01MascotasIntegrationTest {
    private static final String RUTA = "/api/v1/mascotas";
    @Autowired MockMvc http;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired AmbitosDePrueba ambitos;

    private UUID titular;
    private UUID ajeno;
    private UUID etiqueta;
    private final Set<UUID> etiquetasDePrueba = new HashSet<>();
    private final Set<String> especiesDePrueba = new HashSet<>();

    @BeforeEach
    void prepararActoresYCatalogoFicticio() {
        comprobarEsquemaDePruebas();
        titular = UUID.randomUUID();
        ajeno = UUID.randomUUID();
        ambitos.porUsuario.clear();
        ambitos.identidadesRevocadas.clear();
        etiqueta = etiqueta(true);
    }

    @AfterEach
    void limpiarSoloLosDatosDeEsteCaso() {
        comprobarEsquemaDePruebas();
        if (titular == null || ajeno == null) return;
        // Primero las tablas que referencian a mascota; los catálogos iniciales no se tocan.
        jdbc.update("""
                DELETE FROM mascota_desactivacion WHERE mascota_id IN
                (SELECT id FROM mascota WHERE responsable_usuario_id IN (?, ?))
                """, titular, ajeno);
        jdbc.update("""
                DELETE FROM mascota_operacion_idempotente WHERE actor_usuario_id IN (?, ?)
                OR recurso_id IN (SELECT id FROM mascota WHERE responsable_usuario_id IN (?, ?))
                """, titular, ajeno, titular, ajeno);
        jdbc.update("""
                DELETE FROM mascota_comportamiento WHERE mascota_id IN
                (SELECT id FROM mascota WHERE responsable_usuario_id IN (?, ?))
                """, titular, ajeno);
        jdbc.update("DELETE FROM mascota WHERE responsable_usuario_id IN (?, ?)", titular, ajeno);
        for (UUID id : etiquetasDePrueba) jdbc.update("DELETE FROM etiqueta_comportamiento WHERE id = ?", id);
        for (String codigo : especiesDePrueba) jdbc.update("DELETE FROM especie WHERE codigo = ?", codigo);
        ambitos.porUsuario.clear();
        ambitos.identidadesRevocadas.clear();
    }

    @Test
    void creaLunaYLaRecuperaPorHttpConLosValoresInicialesDelServidor() throws Exception {
        Map<String, Object> creada = crear(titular, alta(), UUID.randomUUID());
        UUID id = id(creada);
        assertThat(creada).containsEntry("nombre", "Luna").containsEntry("version", 0)
                .containsEntry("activa", true).containsEntry("disponibilidad", "NO_DISPONIBLE")
                .containsEntry("responsableUsuarioId", titular.toString());
        assertThat(creada.get("organizacionId")).isNull();
        assertThat(creada.get("fotoUrl")).isNull();

        http.perform(get(RUTA + "/{id}", id).with(user(titular.toString())))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.nombre").value("Luna"))
                .andExpect(jsonPath("$.comportamientoIds", contains(etiqueta.toString())))
                .andExpect(jsonPath("$.comportamientos[0].id").value(etiqueta.toString()))
                .andExpect(jsonPath("$.comportamientos[0].nombre").value("Etiqueta ficticia B01"))
                .andExpect(jsonPath("$.expediente").doesNotExist())
                .andExpect(jsonPath("$.contactos").doesNotExist());
        assertThat(jdbc.queryForObject("SELECT nombre FROM mascota WHERE id = ?", String.class, id))
                .isEqualTo("Luna");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void aceptaLosLimitesDelNombreYEdadCero(int longitud) throws Exception {
        Map<String, Object> solicitud = alta();
        solicitud.put("nombre", "  " + "🐾".repeat(longitud) + "  ");
        solicitud.put("edadEstimadaMeses", 0);
        Map<String, Object> creada = crear(titular, solicitud, UUID.randomUUID());
        assertThat(creada).containsEntry("nombre", "🐾".repeat(longitud))
                .containsEntry("edadEstimadaMeses", 0);
    }

    @Test
    void listaSoloLasPropiasYRespetaLaPaginacion() throws Exception {
        crear(titular, alta(), UUID.randomUUID());
        crear(titular, alta(), UUID.randomUUID());
        crear(ajeno, alta(), UUID.randomUUID());

        http.perform(get(RUTA).with(user(titular.toString())).param("pagina", "0").param("limite", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.limite").value(1))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.elementos.length()").value(1))
                .andExpect(jsonPath("$.elementos[0].responsableUsuarioId").value(titular.toString()));
        http.perform(get(RUTA).with(user(UUID.randomUUID().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.elementos.length()").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "101", "0", "texto"})
    void rechazaLimitesDePaginaInvalidos(String limite) throws Exception {
        http.perform(get(RUTA).with(user(titular.toString())).param("limite", limite))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaPaginaNegativa() throws Exception {
        http.perform(get(RUTA).with(user(titular.toString())).param("pagina", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinSesionNoSePuedeLeerNiCrearUnPerfil() throws Exception {
        http.perform(get(RUTA)).andExpect(status().isUnauthorized());
        http.perform(get(RUTA + "/{id}", UUID.randomUUID())).andExpect(status().isUnauthorized());
        http.perform(post(RUTA).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID()).content(json.writeValueAsString(alta())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unaSesionNoSustituyeElTokenCsrfDeLasEscrituras() throws Exception {
        int antes = cantidadMascotas();
        http.perform(post(RUTA).with(user(titular.toString())).contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID()).content(json.writeValueAsString(alta())))
                .andExpect(status().isForbidden());
        assertThat(cantidadMascotas()).isEqualTo(antes);
    }

    @Test
    void conocerElIdAjenoNoPermiteLeerEditarNiDesactivar() throws Exception {
        UUID id = id(crear(titular, alta(), UUID.randomUUID()));
        http.perform(get(RUTA + "/{id}", id).with(user(ajeno.toString())))
                .andExpect(status().isNotFound());
        http.perform(get(RUTA + "/{id}", UUID.randomUUID()).with(user(ajeno.toString())))
                .andExpect(status().isNotFound());
        editar(ajeno, id, edicion(0)).andExpect(status().isNotFound());
        desactivar(ajeno, id, 0, "Intento ajeno").andExpect(status().isNotFound());
        assertThat(jdbc.queryForMap("SELECT nombre, activa, version FROM mascota WHERE id = ?", id))
                .containsEntry("nombre", "Luna").containsEntry("activa", true).containsEntry("version", 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"responsableUsuarioId", "disponibilidad", "clinica", "fotoUrl", "id", "creadoEn", "version", "activa"})
    void elClienteNoPuedeIntroducirCamposReservados(String campo) throws Exception {
        int antes = cantidadMascotas();
        Map<String, Object> solicitud = alta();
        solicitud.put(campo, campo.equals("disponibilidad") ? "ADOPTADA" : UUID.randomUUID().toString());
        enviarAlta(titular, solicitud, UUID.randomUUID()).andExpect(status().isBadRequest());
        assertThat(cantidadMascotas()).isEqualTo(antes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"nombre", "especie", "edadEstimadaMeses", "tamano", "energia", "comportamientoIds"})
    void rechazaCamposObligatoriosAusentes(String campo) throws Exception {
        Map<String, Object> solicitud = alta();
        solicitud.remove(campo);
        enviarAlta(titular, solicitud, UUID.randomUUID()).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rechazaNombreVacio(String nombre) throws Exception {
        Map<String, Object> solicitud = alta();
        solicitud.put("nombre", nombre);
        enviarAlta(titular, solicitud, UUID.randomUUID()).andExpect(status().isBadRequest());
    }

    @Test
    void rechazaNombreMayorDeCienCaracteres() throws Exception {
        Map<String, Object> solicitud = alta();
        solicitud.put("nombre", "a".repeat(101));
        enviarAlta(titular, solicitud, UUID.randomUUID()).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "8.5"})
    void noTruncaLaEdadDecimalNiAceptaUnaEdadNegativa(String edad) throws Exception {
        String cuerpo = json.writeValueAsString(alta()).replace("\"edadEstimadaMeses\":8", "\"edadEstimadaMeses\":" + edad);
        http.perform(post(RUTA).with(user(titular.toString())).with(csrf())
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaTamanoYEnergiaFueraDelCatalogo() throws Exception {
        Map<String, Object> solicitud = alta();
        solicitud.put("tamano", "GIGANTE");
        enviarAlta(titular, solicitud, UUID.randomUUID()).andExpect(status().isBadRequest());
        solicitud.put("tamano", "PEQUENO");
        solicitud.put("energia", "INFINITA");
        enviarAlta(titular, solicitud, UUID.randomUUID()).andExpect(status().isBadRequest());
    }

    @Test
    void rechazaEtiquetasVaciasDuplicadasOInexistentes() throws Exception {
        Map<String, Object> solicitud = alta();
        solicitud.put("comportamientoIds", List.of());
        enviarAlta(titular, solicitud, UUID.randomUUID()).andExpect(status().isBadRequest());
        solicitud.put("comportamientoIds", List.of(etiqueta, etiqueta));
        enviarAlta(titular, solicitud, UUID.randomUUID()).andExpect(status().isBadRequest());
        solicitud.put("comportamientoIds", List.of(UUID.randomUUID()));
        enviarAlta(titular, solicitud, UUID.randomUUID()).andExpect(status().isBadRequest());
    }

    @Test
    void unaEtiquetaInactivaNoSePuedeAsignar() throws Exception {
        UUID clave = UUID.randomUUID();
        int antes = cantidadMascotas();
        Map<String, Object> solicitud = alta();
        solicitud.put("comportamientoIds", List.of(etiqueta(false)));
        enviarAlta(titular, solicitud, clave).andExpect(status().isBadRequest());
        assertThat(cantidadMascotas()).isEqualTo(antes);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM mascota_operacion_idempotente WHERE actor_usuario_id = ? AND clave = ?
                """, Integer.class, titular, clave)).isZero();
        // La reserva fallida también se revierte: la intención corregida puede usar su clave.
        crear(titular, alta(), clave);
    }

    @Test
    void permiteOtraEspecieCuandoExisteEnElCatalogoActivo() throws Exception {
        String codigo = especie(true);
        http.perform(get("/api/v1/catalogos/mascotas"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.especies", hasItem(codigo)));
        Map<String, Object> solicitud = alta();
        solicitud.put("especie", codigo);
        assertThat(crear(titular, solicitud, UUID.randomUUID())).containsEntry("especie", codigo);
    }

    @Test
    void unaEspecieInactivaNoSeOfreceNiSePuedeAsignar() throws Exception {
        String codigo = especie(false);
        http.perform(get("/api/v1/catalogos/mascotas"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.especies", not(hasItem(codigo))));
        Map<String, Object> solicitud = alta();
        solicitud.put("especie", codigo);
        enviarAlta(titular, solicitud, UUID.randomUUID()).andExpect(status().isBadRequest());
    }

    @Test
    void rechazaEspecieQueNoExisteEnElCatalogo() throws Exception {
        Map<String, Object> solicitud = alta();
        solicitud.put("especie", "INEXISTENTE");
        enviarAlta(titular, solicitud, UUID.randomUUID()).andExpect(status().isBadRequest());
    }

    @Test
    void reintentarElAltaConLaMismaClaveNoDuplicaLaMascota() throws Exception {
        UUID clave = UUID.randomUUID();
        int antes = cantidadMascotas();
        UUID id = id(crear(titular, alta(), clave));
        assertThat(id(crear(titular, alta(), clave))).isEqualTo(id);
        assertThat(cantidadMascotas()).isEqualTo(antes + 1);
    }

    @Test
    void laMismaIntencionNormalizaEspaciosYOrdenDeEtiquetas() throws Exception {
        UUID segundaEtiqueta = etiqueta(true);
        UUID clave = UUID.randomUUID();
        Map<String, Object> inicial = alta();
        inicial.put("nombre", "  Luna  ");
        inicial.put("comportamientoIds", List.of(etiqueta, segundaEtiqueta));
        UUID id = id(crear(titular, inicial, clave));
        Map<String, Object> reenvio = alta();
        reenvio.put("comportamientoIds", List.of(segundaEtiqueta, etiqueta));
        assertThat(id(crear(titular, reenvio, clave))).isEqualTo(id);
    }

    @Test
    void reutilizarLaClaveParaOtrosDatosDevuelveConflicto() throws Exception {
        UUID clave = UUID.randomUUID();
        UUID id = id(crear(titular, alta(), clave));
        Map<String, Object> distintos = alta();
        distintos.put("nombre", "Otro nombre");
        enviarAlta(titular, distintos, clave).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT nombre FROM mascota WHERE id = ?", String.class, id)).isEqualTo("Luna");
    }

    @Test
    void laClaveDeIdempotenciaEstaSeparadaPorActor() throws Exception {
        UUID clave = UUID.randomUUID();
        UUID primera = id(crear(titular, alta(), clave));
        UUID segunda = id(crear(ajeno, alta(), clave));
        assertThat(segunda).isNotEqualTo(primera);
    }

    @Test
    void elAltaExigeUnaClaveDeIdempotenciaValida() throws Exception {
        http.perform(post(RUTA).with(user(titular.toString())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(alta())))
                .andExpect(status().isBadRequest());
        http.perform(post(RUTA).with(user(titular.toString())).with(csrf())
                        .header("Idempotency-Key", "no-es-un-uuid")
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(alta())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editaLunaYRecuperaLosCambiosEnOtraConsulta() throws Exception {
        UUID id = id(crear(titular, alta(), UUID.randomUUID()));
        UUID nuevaEtiqueta = etiqueta(true);
        Map<String, Object> cambios = edicion(0);
        cambios.put("edadEstimadaMeses", 9);
        cambios.put("energia", "ALTA");
        cambios.put("comportamientoIds", List.of(nuevaEtiqueta));
        editar(titular, id, cambios).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.version").value(1));
        http.perform(get(RUTA + "/{id}", id).with(user(titular.toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.edadEstimadaMeses").value(9))
                .andExpect(jsonPath("$.energia").value("ALTA"))
                .andExpect(jsonPath("$.comportamientoIds", contains(nuevaEtiqueta.toString())))
                .andExpect(jsonPath("$.version").value(1));
        assertThat(jdbc.queryForList("SELECT etiqueta_id FROM mascota_comportamiento WHERE mascota_id = ?",
                UUID.class, id)).containsExactly(nuevaEtiqueta);
    }

    @Test
    void laEdicionObsoletaNoPisaLosCambiosYaGuardados() throws Exception {
        UUID id = id(crear(titular, alta(), UUID.randomUUID()));
        Map<String, Object> primero = edicion(0);
        primero.put("nombre", "Luna actualizada");
        editar(titular, id, primero).andExpect(status().isOk());
        Map<String, Object> obsoleto = edicion(0);
        obsoleto.put("nombre", "Cambio obsoleto");
        editar(titular, id, obsoleto).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VERSION_DESACTUALIZADA"));
        assertThat(jdbc.queryForMap("SELECT nombre, version FROM mascota WHERE id = ?", id))
                .containsEntry("nombre", "Luna actualizada").containsEntry("version", 1);
    }

    @Test
    void editarNoPermiteCambiarPropietarioNiDisponibilidad() throws Exception {
        UUID id = id(crear(titular, alta(), UUID.randomUUID()));
        Map<String, Object> cambios = edicion(0);
        cambios.put("responsableUsuarioId", ajeno);
        editar(titular, id, cambios).andExpect(status().isBadRequest());
        cambios.remove("responsableUsuarioId");
        cambios.put("disponibilidad", "ADOPTADA");
        editar(titular, id, cambios).andExpect(status().isBadRequest());
    }

    @Test
    void omitirUnaEtiquetaRetiradaEnPutLaQuitaDeLaSeleccion() throws Exception {
        UUID vigente = etiqueta(true);
        Map<String, Object> solicitud = alta();
        solicitud.put("comportamientoIds", List.of(etiqueta, vigente));
        UUID id = id(crear(titular, solicitud, UUID.randomUUID()));
        jdbc.update("UPDATE etiqueta_comportamiento SET activa = false WHERE id = ?", etiqueta);

        Map<String, Object> cambios = edicion(0);
        cambios.put("comportamientoIds", List.of(vigente));
        editar(titular, id, cambios).andExpect(status().isOk())
                .andExpect(jsonPath("$.comportamientoIds", contains(vigente.toString())));
        assertThat(jdbc.queryForList("SELECT etiqueta_id FROM mascota_comportamiento WHERE mascota_id = ?",
                UUID.class, id)).containsExactly(vigente);
    }

    @Test
    void unaEtiquetaRetiradaYaAsignadaSeConservaSiElResponsableLaMantiene() throws Exception {
        UUID id = id(crear(titular, alta(), UUID.randomUUID()));
        jdbc.update("UPDATE etiqueta_comportamiento SET activa = false WHERE id = ?", etiqueta);
        editar(titular, id, edicion(0)).andExpect(status().isOk())
                .andExpect(jsonPath("$.comportamientoIds", contains(etiqueta.toString())));
    }

    @Test
    void desactivaConMotivoYConservaElIdYLasRelaciones() throws Exception {
        UUID id = id(crear(titular, alta(), UUID.randomUUID()));
        desactivar(titular, id, 0, "Perfil duplicado de prueba").andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.activa").value(false))
                .andExpect(jsonPath("$.version").value(1));
        assertThat(jdbc.queryForMap("SELECT id, activa FROM mascota WHERE id = ?", id))
                .containsEntry("id", id).containsEntry("activa", false);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mascota_comportamiento WHERE mascota_id = ?",
                Integer.class, id)).isEqualTo(1);
        Map<String, Object> auditoria = jdbc.queryForMap("""
                SELECT actor_usuario_id, fecha, motivo, version_anterior, version_nueva
                FROM mascota_desactivacion WHERE mascota_id = ?
                """, id);
        assertThat(auditoria).containsEntry("actor_usuario_id", titular)
                .containsEntry("motivo", "Perfil duplicado de prueba")
                .containsEntry("version_anterior", 0).containsEntry("version_nueva", 1);
        assertThat(auditoria.get("fecha")).isNotNull();
        http.perform(get(RUTA).with(user(titular.toString())).param("activas", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.elementos[*].id", hasItem(id.toString())));
        http.perform(get(RUTA).with(user(titular.toString())).param("activas", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.elementos[*].id", not(hasItem(id.toString()))));
        editar(titular, id, edicion(1)).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT disponibilidad FROM mascota WHERE id = ?", String.class, id))
                .isEqualTo("NO_DISPONIBLE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void desactivarExigeUnMotivo(String motivo) throws Exception {
        UUID id = id(crear(titular, alta(), UUID.randomUUID()));
        desactivar(titular, id, 0, motivo).andExpect(status().isBadRequest());
    }

    @Test
    void elMotivoAceptaQuinientosCaracteresYRechazaQuinientosUno() throws Exception {
        UUID id = id(crear(titular, alta(), UUID.randomUUID()));
        desactivar(titular, id, 0, "a".repeat(501)).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT activa FROM mascota WHERE id = ?", Boolean.class, id)).isTrue();
        desactivar(titular, id, 0, "a".repeat(500)).andExpect(status().isOk());
    }

    @Test
    void desactivarConVersionAntiguaNoCambiaLaMascota() throws Exception {
        UUID id = id(crear(titular, alta(), UUID.randomUUID()));
        editar(titular, id, edicion(0)).andExpect(status().isOk());
        desactivar(titular, id, 0, "Versión anterior").andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT activa FROM mascota WHERE id = ?", Boolean.class, id)).isTrue();
    }

    @Test
    void soloUnRepresentanteVigentePuedeCrearEnSuOrganizacion() throws Exception {
        UUID organizacion = UUID.randomUUID();
        ambitos.porUsuario.put(titular, Set.of(organizacion));
        Map<String, Object> solicitud = alta();
        solicitud.put("organizacionId", organizacion);
        UUID id = id(crear(titular, solicitud, UUID.randomUUID()));
        http.perform(get(RUTA + "/{id}", id).with(user(titular.toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.organizacionId").value(organizacion.toString()));
        enviarAlta(ajeno, solicitud, UUID.randomUUID()).andExpect(status().isForbidden());
    }

    @Test
    void otroRepresentanteDeLaMismaOrganizacionPuedeGestionarSinSerElCreador() throws Exception {
        UUID organizacion = UUID.randomUUID();
        ambitos.porUsuario.put(titular, Set.of(organizacion));
        ambitos.porUsuario.put(ajeno, Set.of(organizacion));
        Map<String, Object> solicitud = alta();
        solicitud.put("organizacionId", organizacion);
        UUID id = id(crear(titular, solicitud, UUID.randomUUID()));
        http.perform(get(RUTA + "/{id}", id).with(user(ajeno.toString())))
                .andExpect(status().isOk());
        editar(ajeno, id, edicion(0)).andExpect(status().isOk());
        http.perform(get(RUTA).with(user(ajeno.toString())).param("organizacionId", organizacion.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.elementos[*].id", hasItem(id.toString())));
    }

    @Test
    void revocarElAmbitoImpideElAccesoEnLaSiguientePeticion() throws Exception {
        UUID organizacion = UUID.randomUUID();
        ambitos.porUsuario.put(titular, Set.of(organizacion));
        ambitos.porUsuario.put(ajeno, Set.of(organizacion));
        Map<String, Object> solicitud = alta();
        solicitud.put("organizacionId", organizacion);
        UUID id = id(crear(titular, solicitud, UUID.randomUUID()));
        http.perform(get(RUTA + "/{id}", id).with(user(ajeno.toString()))).andExpect(status().isOk());
        ambitos.porUsuario.remove(ajeno);
        http.perform(get(RUTA + "/{id}", id).with(user(ajeno.toString()))).andExpect(status().isNotFound());
        editar(ajeno, id, edicion(0)).andExpect(status().isNotFound());
        desactivar(ajeno, id, 0, "Ámbito revocado").andExpect(status().isNotFound());
        http.perform(get(RUTA).with(user(ajeno.toString())).param("organizacionId", organizacion.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void representarOtraOrganizacionNoDaAccesoAlPerfilAjeno() throws Exception {
        UUID organizacion = UUID.randomUUID();
        ambitos.porUsuario.put(titular, Set.of(organizacion));
        ambitos.porUsuario.put(ajeno, Set.of(UUID.randomUUID()));
        Map<String, Object> solicitud = alta();
        solicitud.put("organizacionId", organizacion);
        UUID id = id(crear(titular, solicitud, UUID.randomUUID()));
        http.perform(get(RUTA + "/{id}", id).with(user(ajeno.toString()))).andExpect(status().isNotFound());
    }

    @Test
    void revocarAlRepresentanteCreadorTambienLeRetiraElAccesoOrganizativo() throws Exception {
        UUID organizacion = UUID.randomUUID();
        UUID clave = UUID.randomUUID();
        ambitos.porUsuario.put(titular, Set.of(organizacion));
        Map<String, Object> solicitud = alta();
        solicitud.put("organizacionId", organizacion);
        UUID id = id(crear(titular, solicitud, clave));

        // Haber registrado la mascota no convierte al representante en dueño individual.
        ambitos.porUsuario.remove(titular);
        http.perform(get(RUTA + "/{id}", id).with(user(titular.toString()))).andExpect(status().isNotFound());
        editar(titular, id, edicion(0)).andExpect(status().isNotFound());
        desactivar(titular, id, 0, "Representación revocada").andExpect(status().isNotFound());
        http.perform(get(RUTA).with(user(titular.toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.elementos[*].id", not(hasItem(id.toString()))));
        enviarAlta(titular, solicitud, clave).andExpect(status().isForbidden());
        assertThat(jdbc.queryForMap("SELECT nombre, activa, version FROM mascota WHERE id = ?", id))
                .containsEntry("nombre", "Luna").containsEntry("activa", true).containsEntry("version", 0);
    }

    @Test
    void elContextoPublicaSoloLosAmbitosVigentesDeLaSesion() throws Exception {
        UUID organizacion = UUID.randomUUID();
        UUID organizacionAjena = UUID.randomUUID();
        ambitos.porUsuario.put(titular, Set.of(organizacion));
        ambitos.porUsuario.put(ajeno, Set.of(organizacionAjena));
        http.perform(get(RUTA + "/contexto").with(user(titular.toString())))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.usuarioId").value(titular.toString()))
                .andExpect(jsonPath("$.organizaciones.length()").value(1))
                .andExpect(jsonPath("$.organizaciones[0].id").value(organizacion.toString()))
                .andExpect(jsonPath("$.organizaciones[0].nombre").isString());
        http.perform(get(RUTA + "/contexto").with(user(ajeno.toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuarioId").value(ajeno.toString()))
                .andExpect(jsonPath("$.organizaciones[*].id", contains(organizacionAjena.toString())));
        ambitos.porUsuario.remove(titular);
        http.perform(get(RUTA + "/contexto").with(user(titular.toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.organizaciones.length()").value(0));
        http.perform(get(RUTA + "/contexto")).andExpect(status().isUnauthorized());
    }

    @Test
    void retirarLaIdentidadRechazaLaSiguientePeticionAunqueSigaAutenticada() throws Exception {
        UUID id = id(crear(titular, alta(), UUID.randomUUID()));
        ambitos.identidadesRevocadas.add(titular);
        http.perform(get(RUTA + "/contexto").with(user(titular.toString())))
                .andExpect(status().isUnauthorized());
        http.perform(get(RUTA + "/{id}", id).with(user(titular.toString())))
                .andExpect(status().isUnauthorized());
        editar(titular, id, edicion(0)).andExpect(status().isUnauthorized());
        desactivar(titular, id, 0, "Identidad retirada").andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForMap("SELECT nombre, activa, version FROM mascota WHERE id = ?", id))
                .containsEntry("nombre", "Luna").containsEntry("activa", true).containsEntry("version", 0);
    }

    @Test
    void conservaUnaEspecieRetiradaExistentePeroNoPermiteAsignarlaAOtraMascota() throws Exception {
        String codigo = especie(true);
        Map<String, Object> datos = alta();
        datos.put("especie", codigo);
        UUID id = id(crear(titular, datos, UUID.randomUUID()));
        jdbc.update("UPDATE especie SET activa = false WHERE codigo = ?", codigo);

        Map<String, Object> cambios = edicion(0);
        cambios.put("especie", codigo);
        cambios.put("nombre", "Luna con historia");
        editar(titular, id, cambios).andExpect(status().isOk())
                .andExpect(jsonPath("$.especie").value(codigo));
        enviarAlta(titular, datos, UUID.randomUUID()).andExpect(status().isBadRequest());
        UUID otra = id(crear(titular, alta(), UUID.randomUUID()));
        editar(titular, otra, cambios).andExpect(status().isBadRequest());
    }

    @Test
    void reintentarLaDesactivacionNoDuplicaAuditoriaNiAceptaOtraMascotaConLaMismaClave() throws Exception {
        UUID id = id(crear(titular, alta(), UUID.randomUUID()));
        UUID otra = id(crear(titular, alta(), UUID.randomUUID()));
        UUID clave = UUID.randomUUID();
        String cuerpo = json.writeValueAsString(Map.of("versionEsperada", 0, "motivo", "Cierre de prueba"));
        for (int intento = 0; intento < 2; intento++) {
            http.perform(post(RUTA + "/{id}/desactivacion", id).with(user(titular.toString())).with(csrf())
                            .header("Idempotency-Key", clave).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
                    .andExpect(jsonPath("$.activa").value(false));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mascota_desactivacion WHERE mascota_id = ?",
                Integer.class, id)).isEqualTo(1);
        http.perform(post(RUTA + "/{id}/desactivacion", otra).with(user(titular.toString())).with(csrf())
                        .header("Idempotency-Key", clave).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT activa FROM mascota WHERE id = ?", Boolean.class, otra)).isTrue();
    }

    @Test
    void unaClaveDeBajaNoPermiteReleerUnaMascotaTrasRevocarElAmbito() throws Exception {
        UUID organizacion = UUID.randomUUID();
        UUID clave = UUID.randomUUID();
        ambitos.porUsuario.put(titular, Set.of(organizacion));
        Map<String, Object> datos = alta();
        datos.put("organizacionId", organizacion);
        UUID id = id(crear(titular, datos, UUID.randomUUID()));
        String cuerpo = json.writeValueAsString(Map.of("versionEsperada", 0, "motivo", "Baja organizativa"));
        http.perform(post(RUTA + "/{id}/desactivacion", id).with(user(titular.toString())).with(csrf())
                        .header("Idempotency-Key", clave).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk());
        ambitos.porUsuario.remove(titular);
        http.perform(post(RUTA + "/{id}/desactivacion", id).with(user(titular.toString())).with(csrf())
                        .header("Idempotency-Key", clave).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isNotFound());
    }

    private Map<String, Object> alta() {
        Map<String, Object> solicitud = new LinkedHashMap<>();
        solicitud.put("nombre", "Luna");
        solicitud.put("especie", "GATO");
        solicitud.put("edadEstimadaMeses", 8);
        solicitud.put("tamano", "PEQUENO");
        solicitud.put("energia", "MEDIA");
        solicitud.put("comportamientoIds", List.of(etiqueta));
        solicitud.put("organizacionId", null);
        return solicitud;
    }

    private Map<String, Object> edicion(int version) {
        Map<String, Object> solicitud = alta();
        solicitud.remove("organizacionId");
        solicitud.put("versionEsperada", version);
        return solicitud;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> crear(UUID actor, Map<String, Object> solicitud, UUID clave) throws Exception {
        var respuesta = enviarAlta(actor, solicitud, clave).andExpect(status().isCreated())
                .andExpect(header().exists("Location")).andReturn().getResponse();
        String cuerpo = respuesta.getContentAsString(StandardCharsets.UTF_8);
        Map<String, Object> creada = json.readValue(cuerpo, Map.class);
        assertThat(creada.get("id")).isNotNull();
        assertThat(respuesta.getHeader("Location")).isEqualTo(RUTA + "/" + creada.get("id"));
        return creada;
    }

    private ResultActions enviarAlta(UUID actor, Map<String, Object> solicitud, UUID clave) throws Exception {
        return http.perform(post(RUTA).with(user(actor.toString())).with(csrf())
                .header("Idempotency-Key", clave).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(solicitud)));
    }

    private ResultActions editar(UUID actor, UUID id, Map<String, Object> solicitud) throws Exception {
        return http.perform(put(RUTA + "/{id}", id).with(user(actor.toString())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(solicitud)));
    }

    private ResultActions desactivar(UUID actor, UUID id, int version, String motivo) throws Exception {
        return http.perform(post(RUTA + "/{id}/desactivacion", id).with(user(actor.toString())).with(csrf())
                .header("Idempotency-Key", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("versionEsperada", version, "motivo", motivo))));
    }

    private UUID etiqueta(boolean activa) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO etiqueta_comportamiento (id, nombre, activa) VALUES (?, ?, ?)",
                id, "Etiqueta ficticia B01", activa);
        etiquetasDePrueba.add(id);
        return id;
    }

    private String especie(boolean activa) {
        String codigo = "CONEJO_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        jdbc.update("INSERT INTO especie (codigo, nombre, activa) VALUES (?, ?, ?)", codigo, "Conejo de prueba", activa);
        especiesDePrueba.add(codigo);
        return codigo;
    }

    private void comprobarEsquemaDePruebas() {
        assertThat(jdbc.queryForObject("SELECT current_schema()", String.class)).isEqualTo("pruebas");
    }

    private int cantidadMascotas() {
        return jdbc.queryForObject("SELECT count(*) FROM mascota", Integer.class);
    }

    private UUID id(Map<String, Object> mascota) {
        return UUID.fromString(mascota.get("id").toString());
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
        AmbitosDePrueba ambitosDePrueba() {
            return new AmbitosDePrueba();
        }
    }
}
