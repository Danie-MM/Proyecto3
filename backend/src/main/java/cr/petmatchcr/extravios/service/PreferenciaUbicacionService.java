package cr.petmatchcr.extravios.service;
import cr.petmatchcr.extravios.dto.GuardarPreferenciaUbicacionDTO;
import cr.petmatchcr.extravios.dto.PreferenciaUbicacionDTO;
import cr.petmatchcr.extravios.mappers.PreferenciaUbicacionMapper;
import cr.petmatchcr.extravios.model.PreferenciaUbicacion;
import cr.petmatchcr.extravios.repository.PreferenciaUbicacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PreferenciaUbicacionService {

    private final PreferenciaUbicacionRepository repository;
    private final PreferenciaUbicacionMapper mapper;
    private final UsuarioActual usuarioActual;


    @Transactional
    public PreferenciaUbicacionDTO crear(GuardarPreferenciaUbicacionDTO dto) {

        validarDatos(dto);

        UUID usuarioId = usuarioActual.obtenerUsuarioId();

        if (repository.findByUsuarioId(usuarioId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El usuario ya tiene preferencias registradas"
            );
        }

        PreferenciaUbicacion preferencia = new PreferenciaUbicacion();

        preferencia.setUsuarioId(usuarioId);
        preferencia.setCreadoEn(Instant.now());

        mapper.actualizarEntidad(dto, preferencia);

        if (dto.aceptaAvisos()) {
            preferencia.setConsentimientoEn(Instant.now());
        }

        preferencia.setActualizadaEn(Instant.now());

        PreferenciaUbicacion guardada = repository.saveAndFlush(preferencia);

        return mapper.toDTO(guardada);
    }



    @Transactional
    public PreferenciaUbicacionDTO actualizar(GuardarPreferenciaUbicacionDTO dto) {

        validarDatos(dto);

        UUID usuarioId = usuarioActual.obtenerUsuarioId();

        PreferenciaUbicacion preferencia = repository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El usuario no tiene preferencias registradas"
                ));

        boolean aceptabaAvisos = preferencia.isAceptaAvisos();

        mapper.actualizarEntidad(dto, preferencia);

        if (dto.aceptaAvisos() && !aceptabaAvisos) {
            preferencia.setConsentimientoEn(Instant.now());
        }

        if (!dto.aceptaAvisos()) {
            preferencia.setConsentimientoEn(null);
        }

        preferencia.setActualizadaEn(Instant.now());

        PreferenciaUbicacion guardada = repository.saveAndFlush(preferencia);

        return mapper.toDTO(guardada);
    }


    private void validarDatos(GuardarPreferenciaUbicacionDTO dto) {

        if (dto == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El cuerpo de la solicitud es obligatorio"
            );
        }

        validarCoordenadas(dto.latitud(), dto.longitud());

        if (dto.referencia() != null && dto.referencia().length() > 200) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La referencia no puede superar 200 caracteres"
            );
        }
    }

    @Transactional(readOnly = true)
    public PreferenciaUbicacionDTO consultar() {

        UUID usuarioId = usuarioActual.obtenerUsuarioId();

        PreferenciaUbicacion preferencia = repository
                .findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El usuario no tiene preferencias registradas"
                ));

        return mapper.toDTO(preferencia);
    }


    private void validarCoordenadas(
            BigDecimal latitud,
            BigDecimal longitud
    ) {

        if ((latitud == null) != (longitud == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La latitud y longitud deben indicarse juntas"
            );
        }

        if (latitud != null) {

            if (latitud.compareTo(BigDecimal.valueOf(-90)) < 0
                    || latitud.compareTo(BigDecimal.valueOf(90)) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La latitud debe estar entre -90 y 90"
                );
            }

            if (longitud.compareTo(BigDecimal.valueOf(-180)) < 0
                    || longitud.compareTo(BigDecimal.valueOf(180)) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La longitud debe estar entre -180 y 180"
                );
            }
        }
    }


    @Transactional
    public void eliminar() {
        UUID usuarioId = usuarioActual.obtenerUsuarioId();

        PreferenciaUbicacion preferencia = repository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El usuario no tiene preferencias registradas"
                ));

        repository.delete(preferencia);
    }

}
