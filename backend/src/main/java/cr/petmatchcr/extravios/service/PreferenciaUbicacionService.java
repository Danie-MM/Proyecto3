package cr.petmatchcr.extravios.service;

import cr.petmatchcr.extravios.dto.ActualizarPreferenciaUbicacionDTO;
import cr.petmatchcr.extravios.dto.GuardarPreferenciaUbicacionDTO;
import cr.petmatchcr.extravios.dto.PreferenciaUbicacionDTO;
import cr.petmatchcr.extravios.mappers.PreferenciaUbicacionMapper;
import cr.petmatchcr.extravios.model.PreferenciaUbicacion;
import cr.petmatchcr.extravios.repository.IntentosUbicacionRepository;
import cr.petmatchcr.extravios.repository.PreferenciaUbicacionRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Mantiene una preferencia privada por cuenta con reenvíos y edición concurrente protegidos. */
@Service
@RequiredArgsConstructor
public class PreferenciaUbicacionService {
    private final PreferenciaUbicacionRepository repository;
    private final PreferenciaUbicacionMapper mapper;
    private final UsuarioActual usuarioActual;
    private final IntentosUbicacionRepository intentos;
    private final ObjectMapper json;

    @Transactional(readOnly = true)
    public PreferenciaUbicacionDTO consultar() {
        UUID usuarioId = usuarioActual.obtenerUsuarioId();
        return repository.findByUsuarioId(usuarioId).map(mapper::toDTO)
                .orElseGet(() -> PreferenciaUbicacionDTO.sinConfigurar(usuarioId));
    }

    @Transactional
    public PreferenciaUbicacionDTO crear(GuardarPreferenciaUbicacionDTO dto, UUID clave) {
        UUID usuarioId = usuarioActual.obtenerUsuarioId();
        validarDatos(dto);
        if (clave == null) throw error(400, "VALIDACION", "La clave de reintento es obligatoria.");
        String huella = huella(dto);
        var intento = intentos.reservar(usuarioId, clave, huella);
        if (!huella.equals(intento.huella())) {
            throw error(409, "CLAVE_REUTILIZADA", "Esa clave ya se utilizó con otros datos.");
        }
        if (intento.preferenciaId() != null) {
            // No devolver una preferencia eliminada ni recrearla con una intención antigua.
            repository.findById(intento.preferenciaId()).filter(p -> usuarioId.equals(p.getUsuarioId()))
                    .orElseThrow(() -> error(409, "CONFLICTO", "La preferencia de ese intento ya fue retirada."));
            return json.readValue(intento.respuestaJson(), PreferenciaUbicacionDTO.class);
        }
        if (repository.findByUsuarioId(usuarioId).isPresent()) {
            throw error(409, "CONFLICTO", "Ya tenés una preferencia registrada. Consultala para editarla.");
        }
        var preferencia = new PreferenciaUbicacion();
        preferencia.setUsuarioId(usuarioId);
        preferencia.setCreadoEn(Instant.now().truncatedTo(ChronoUnit.MICROS));
        aplicarDatos(dto, preferencia);
        var respuesta = mapper.toDTO(repository.saveAndFlush(preferencia));
        intentos.completar(usuarioId, clave, respuesta.id(), json.writeValueAsString(respuesta));
        return respuesta;
    }

    @Transactional
    public PreferenciaUbicacionDTO actualizar(ActualizarPreferenciaUbicacionDTO dto) {
        UUID usuarioId = usuarioActual.obtenerUsuarioId();
        if (dto == null) throw error(400, "VALIDACION", "El cuerpo de la solicitud es obligatorio.");
        var datos = dto.datos();
        validarDatos(datos);
        validarVersion(dto.versionEsperada());
        var preferencia = buscarPropia(usuarioId);
        comprobarVersion(preferencia, dto.versionEsperada());
        aplicarDatos(datos, preferencia);
        // @Version también protege la carrera que ocurra después de comprobar versionEsperada.
        return mapper.toDTO(repository.saveAndFlush(preferencia));
    }

    @Transactional
    public void eliminar(Integer versionEsperada) {
        UUID usuarioId = usuarioActual.obtenerUsuarioId();
        validarVersion(versionEsperada);
        var preferencia = buscarPropia(usuarioId);
        comprobarVersion(preferencia, versionEsperada);
        repository.delete(preferencia);
        repository.flush();
    }

    private PreferenciaUbicacion buscarPropia(UUID usuarioId) {
        return repository.findByUsuarioId(usuarioId).orElseThrow(() -> error(
                404, "NO_ENCONTRADO", "Todavía no tenés preferencias registradas."));
    }

    private void aplicarDatos(GuardarPreferenciaUbicacionDTO dto, PreferenciaUbicacion preferencia) {
        boolean aceptabaAvisos = preferencia.isAceptaAvisos();
        mapper.actualizarEntidad(dto, preferencia);
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.MICROS);
        // Conserva también cuándo se retiró la aceptación; una preferencia nueva sin aceptación sigue en null.
        if (dto.aceptaAvisos() != aceptabaAvisos) preferencia.setConsentimientoEn(ahora);
        preferencia.setActualizadaEn(ahora);
    }

    private void validarDatos(GuardarPreferenciaUbicacionDTO dto) {
        if (dto == null || dto.aceptaAvisos() == null) {
            throw error(400, "VALIDACION", "Indicá expresamente si aceptás recibir avisos.");
        }
        if ((dto.latitud() == null) != (dto.longitud() == null)) {
            throw error(400, "VALIDACION", "La latitud y longitud deben indicarse juntas.");
        }
        validarCoordenada(dto.latitud(), 90, "latitud");
        validarCoordenada(dto.longitud(), 180, "longitud");
        if (dto.referencia() != null && dto.referencia().length() > 200) {
            throw error(400, "VALIDACION", "La referencia no puede superar 200 caracteres.");
        }
    }

    private void validarCoordenada(BigDecimal valor, int limite, String campo) {
        if (valor == null) return;
        if (valor.abs().compareTo(BigDecimal.valueOf(limite)) > 0 || valor.scale() > 6) {
            throw error(400, "VALIDACION", "Revisá la " + campo + ": rango válido y hasta seis decimales.");
        }
    }

    private void validarVersion(Integer versionEsperada) {
        if (versionEsperada == null || versionEsperada < 0) {
            throw error(400, "VALIDACION", "La versión de la última consulta es obligatoria.");
        }
    }

    private void comprobarVersion(PreferenciaUbicacion preferencia, Integer versionEsperada) {
        if (!versionEsperada.equals(preferencia.getVersion())) {
            throw error(409, "VERSION_DESACTUALIZADA", "La ubicación cambió. Recargá antes de editar.");
        }
    }

    private String huella(GuardarPreferenciaUbicacionDTO dto) {
        try {
            byte[] entrada = json.writeValueAsString(dto).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(entrada));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible.", ex);
        }
    }

    private ErrorUbicacion error(int estado, String codigo, String mensaje) {
        return new ErrorUbicacion(estado, codigo, mensaje);
    }
}
