package cr.petmatchcr.extravios.service;

import cr.petmatchcr.extravios.dto.CrearReporteExtravioDTO;
import cr.petmatchcr.extravios.dto.ReporteExtravioDTO;
import cr.petmatchcr.extravios.mappers.ReporteExtravioMapper;
import cr.petmatchcr.extravios.model.EstadoReporteExtravio;
import cr.petmatchcr.extravios.model.MedioContacto;
import cr.petmatchcr.extravios.repository.IntentosReporteRepository;
import cr.petmatchcr.extravios.repository.ReporteExtravioRepository;
import cr.petmatchcr.extravios.repository.TrabajoAvisoRepository;
import cr.petmatchcr.mascotas.api.MascotasParaExtravios;

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

/**
 * Gestiona la publicación de reportes de mascotas
 * extraviadas por sus responsables autorizados.
 */
@Service
@RequiredArgsConstructor
public class ReporteExtravioService {

    private final ReporteExtravioRepository repository;
    private final ReporteExtravioMapper mapper;
    private final UsuarioActual usuarioActual;
    private final MascotasParaExtravios mascotas;
    private final IntentosReporteRepository intentos;
    private final TrabajoAvisoRepository trabajoAviso;
    private final ObjectMapper json;

    private static final int RADIO_AVISO_METROS = 3000;

    /**
     * Resultado de una solicitud de publicación.
     */
    public record ResultadoCreacion(
            ReporteExtravioDTO reporte,
            int estadoHttp
    ) {}

    /**
     * Publica un reporte confirmado o recupera
     * el resultado de una publicación anterior.
     *
     * @param dto datos del reporte
     * @param clave clave de idempotencia
     * @return reporte y estado HTTP correspondiente
     */
    @Transactional
    public ResultadoCreacion crear(
            CrearReporteExtravioDTO dto,
            UUID clave
    ) {
        UUID usuarioId = usuarioActual.obtenerUsuarioId();

        validarDatos(dto);

        if (clave == null) {
            throw error(400, "VALIDACION",
                    "La clave de reintento es obligatoria.");
        }

        // Valida que la mascota esté activa y que
        // el usuario sea su responsable.
        mascotas.consultarResumenParaExtravio(dto.mascotaId());

        String huella = huella(dto);

        var intento = intentos.reservar(
                usuarioId, clave, huella
        );

        if (!huella.equals(intento.huella())) {
            throw error(409, "CLAVE_REUTILIZADA",
                    "Esa clave ya se utilizó con otros datos.");
        }

        // Recupera el resultado original de un reintento.
        if (intento.reporteId() != null) {

            repository.findById(intento.reporteId())
                    .filter(reporte -> usuarioId.equals(
                            reporte.getCreadoPorUsuarioId()))
                    .orElseThrow(() -> error(
                            409,
                            "CONFLICTO",
                            "El reporte de ese intento no está disponible."
                    ));

            var respuesta = json.readValue(
                    intento.respuestaJson(),
                    ReporteExtravioDTO.class
            );

            return new ResultadoCreacion(
                    respuesta,
                    intento.estadoHttp()
            );
        }

        // Serializa publicaciones para una misma mascota.
        intentos.bloquearMascota(dto.mascotaId());

        // Comprueba nuevamente la autorización después
        // de adquirir el bloqueo.
        mascotas.consultarResumenParaExtravio(dto.mascotaId());

        var existente = repository.findByMascotaIdAndEstado(
                dto.mascotaId(),
                EstadoReporteExtravio.ACTIVO
        );

        if (existente.isPresent()) {

            var reporte = existente.get();

            if (!usuarioId.equals(
                    reporte.getCreadoPorUsuarioId())) {
                throw error(409, "CONFLICTO",
                        "La mascota ya tiene un reporte activo.");
            }

            var respuesta = mapper.toDTO(reporte);

            intentos.completar(
                    usuarioId,
                    clave,
                    respuesta.id(),
                    json.writeValueAsString(respuesta),
                    200
            );

            return new ResultadoCreacion(respuesta, 200);
        }

        Instant ahora = Instant.now()
                .truncatedTo(ChronoUnit.MICROS);

        var reporte = mapper.toEntity(dto);

        reporte.setCreadoPorUsuarioId(usuarioId);

        reporte.setLatitudOrigenAviso(dto.latitud());
        reporte.setLongitudOrigenAviso(dto.longitud());

        reporte.setRadioMetros(RADIO_AVISO_METROS);
        reporte.setEstado(EstadoReporteExtravio.ACTIVO);

        reporte.setDescripcion(dto.descripcion().strip());
        reporte.setContacto(dto.contacto().strip());

        reporte.setContactoAutorizadoEn(ahora);
        reporte.setCreadoEn(ahora);
        reporte.setActualizadoEn(ahora);

        var respuesta = mapper.toDTO(
                repository.saveAndFlush(reporte)
        );

        // Registra el trabajo pendiente de avisos por proximidad.
        trabajoAviso.registrarPendiente(respuesta.id());

        intentos.completar(
                usuarioId,
                clave,
                respuesta.id(),
                json.writeValueAsString(respuesta),
                201
        );

        return new ResultadoCreacion(respuesta, 201);
    }

    /**
     * Valida los datos necesarios para publicar
     * un reporte de extravío.
     */
    private void validarDatos(CrearReporteExtravioDTO dto) {

        if (dto == null) {
            throw error(400, "VALIDACION",
                    "El cuerpo de la solicitud es obligatorio.");
        }

        if (dto.mascotaId() == null) {
            throw error(400, "VALIDACION",
                    "Debés seleccionar una mascota.");
        }

        if (dto.fechaExtravio() == null
                || dto.fechaExtravio().isAfter(
                Instant.now().truncatedTo(ChronoUnit.MICROS))) {

            throw error(400, "VALIDACION",
                    "La fecha de extravío debe ser válida "
                            + "y no puede estar en el futuro.");
        }

        validarCoordenada(dto.latitud(), 90, "latitud");
        validarCoordenada(dto.longitud(), 180, "longitud");

        if (dto.descripcion() == null
                || dto.descripcion().length() > 1000
                || dto.descripcion().strip().length() < 10) {

            throw error(400, "VALIDACION",
                    "La descripción debe tener entre "
                            + "10 y 1000 caracteres.");
        }

        if (!Boolean.TRUE.equals(dto.autorizaContacto())) {
            throw error(400, "VALIDACION",
                    "Debés autorizar la publicación del contacto.");
        }

        if (!Boolean.TRUE.equals(dto.confirmado())) {
            throw error(400, "VALIDACION",
                    "Debés confirmar la publicación del reporte.");
        }

        validarContacto(dto.tipoContacto(), dto.contacto());
    }

    /**
     * Comprueba el rango y la precisión de una coordenada.
     */
    private void validarCoordenada(
            BigDecimal valor,
            int limite,
            String campo
    ) {
        if (valor == null
                || valor.abs().compareTo(
                BigDecimal.valueOf(limite)) > 0
                || valor.scale() > 6) {

            throw error(400, "VALIDACION",
                    "Revisá la " + campo
                            + ": rango válido y hasta seis decimales.");
        }
    }

    /**
     * Valida el medio de contacto autorizado.
     */
    private void validarContacto(
            MedioContacto tipo,
            String contacto
    ) {
        if (tipo == null
                || contacto == null
                || contacto.isBlank()
                || contacto.length() > 254) {

            throw error(400, "VALIDACION",
                    "Debés indicar un medio de contacto válido.");
        }

        String valor = contacto.strip();

        if (tipo == MedioContacto.CORREO
                && !valor.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {

            throw error(400, "VALIDACION",
                    "El correo electrónico no es válido.");
        }

        if (tipo == MedioContacto.TELEFONO
                && !valor.matches("^\\+[1-9][0-9]{7,14}$")) {

            throw error(400, "VALIDACION",
                    "Indicá el teléfono en formato internacional.");
        }
    }

    /**
     * Genera una huella SHA-256 de la solicitud.
     */
    private String huella(CrearReporteExtravioDTO dto) {
        try {
            byte[] entrada = json.writeValueAsString(dto)
                    .getBytes(StandardCharsets.UTF_8);

            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(entrada)
            );

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 no está disponible.", ex
            );
        }
    }

    private ErrorReporteExtravio error(
            int estado,
            String codigo,
            String mensaje
    ) {
        return new ErrorReporteExtravio(
                estado, codigo, mensaje
        );
    }
}
