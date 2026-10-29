package cr.petmatchcr.entorno;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** Datos que la pantalla puede enviar; id y fecha los decide el servidor. */
@JsonDeserialize(using = PruebaEntornoSolicitudDeserializer.class)
public record PruebaEntornoSolicitud(String texto) {
}
