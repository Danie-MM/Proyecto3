package cr.petmatchcr.entorno;

import cr.petmatchcr.comun.ValidacionException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Aplica las reglas del contrato antes de guardar o consultar. */
@Service
@Profile({"local", "test"})
public class PruebaEntornoService {

    private final PruebaEntornoRepository repository;

    /** Recibe el repositorio que ejecuta las rutinas de PostgreSQL. */
    public PruebaEntornoService(PruebaEntornoRepository repository) {
        this.repository = repository;
    }

    /** Normaliza el texto y crea el UUID y la fecha del registro. */
    @Transactional
    public PruebaEntornoRespuesta crear(PruebaEntornoSolicitud solicitud) {
        if (solicitud == null || solicitud.texto() == null) {
            throw new ValidacionException("texto", "Es obligatorio.");
        }
        String texto = solicitud.texto().strip();
        int caracteres = texto.codePointCount(0, texto.length());
        if (caracteres == 0) {
            throw new ValidacionException("texto", "Es obligatorio.");
        }
        if (caracteres > 80) {
            throw new ValidacionException("texto", "Debe tener entre 1 y 80 caracteres.");
        }
        UUID id = UUID.randomUUID();
        // PostgreSQL guarda microsegundos: POST y GET deben devolver la misma fecha.
        Instant creadoEn = Instant.now().truncatedTo(ChronoUnit.MICROS);
        repository.crear(id, texto, creadoEn);
        return new PruebaEntornoRespuesta(id, texto, creadoEn);
    }

    /** Valida el identificador y busca el registro real, sin respuestas ficticias. */
    @Transactional(readOnly = true)
    public PruebaEntornoRespuesta consultar(String idRecibido) {
        UUID id = validarId(idRecibido);
        return repository.consultar(id).orElseThrow(PruebaEntornoNoEncontradaException::new);
    }

    private UUID validarId(String idRecibido) {
        try {
            UUID id = UUID.fromString(idRecibido);
            // UUID.fromString también acepta algunas formas abreviadas: las rechazamos.
            if (!id.toString().equalsIgnoreCase(idRecibido)) {
                throw new IllegalArgumentException("UUID abreviado");
            }
            return id;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ValidacionException("id", "Debe ser un UUID válido.");
        }
    }
}
