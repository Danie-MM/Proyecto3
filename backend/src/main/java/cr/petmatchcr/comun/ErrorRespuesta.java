package cr.petmatchcr.comun;

import java.util.List;
import java.util.UUID;

/** Mantiene el formato de error compartido con todos los módulos. */
public record ErrorRespuesta(String codigo, String mensaje, List<ErrorCampo> erroresCampos, UUID trazaId) {

    /** Crea un identificador nuevo para poder relacionar el error con los logs. */
    public static ErrorRespuesta crear(String codigo, String mensaje, List<ErrorCampo> erroresCampos) {
        return new ErrorRespuesta(codigo, mensaje, List.copyOf(erroresCampos), UUID.randomUUID());
    }
}
