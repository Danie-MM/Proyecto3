package cr.petmatchcr.entorno;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Iterator;

/** Evita convertir números en texto y rechaza campos que no admite el contrato. */
public class PruebaEntornoSolicitudDeserializer extends StdDeserializer<PruebaEntornoSolicitud> {

    /** Registra el tipo de solicitud que valida este lector de JSON. */
    public PruebaEntornoSolicitudDeserializer() {
        super(PruebaEntornoSolicitud.class);
    }

    /** Lee únicamente un objeto JSON cuyo campo texto sea string o null. */
    @Override
    public PruebaEntornoSolicitud deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        JsonNode body = parser.getCodec().readTree(parser);
        if (!body.isObject()) {
            return context.reportInputMismatch(PruebaEntornoSolicitud.class,
                    "Se esperaba un objeto JSON.");
        }
        if (parser.nextToken() != null) {
            return context.reportInputMismatch(PruebaEntornoSolicitud.class,
                    "La solicitud debe contener un único objeto JSON.");
        }
        Iterator<String> campos = body.fieldNames();
        while (campos.hasNext()) {
            if (!"texto".equals(campos.next())) {
                return context.reportInputMismatch(PruebaEntornoSolicitud.class,
                        "La solicitud solamente admite el campo texto.");
            }
        }
        JsonNode texto = body.get("texto");
        if (texto == null || texto.isNull()) {
            return new PruebaEntornoSolicitud(null);
        }
        if (!texto.isTextual()) {
            return context.reportInputMismatch(PruebaEntornoSolicitud.class,
                    "El campo texto debe ser de tipo string.");
        }
        return new PruebaEntornoSolicitud(texto.textValue());
    }
}
