package cr.petmatchcr.entorno;

import java.time.Instant;
import java.util.UUID;

/** Registro real que recibe Angular después de guardar o consultar. */
public record PruebaEntornoRespuesta(UUID id, String texto, Instant creadoEn) {
}
