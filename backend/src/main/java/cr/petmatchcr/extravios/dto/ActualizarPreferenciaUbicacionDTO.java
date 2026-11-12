package cr.petmatchcr.extravios.dto;

import java.math.BigDecimal;

/** La versión corresponde a la última consulta del usuario. */
public record ActualizarPreferenciaUbicacionDTO(
        BigDecimal latitud,
        BigDecimal longitud,
        String referencia,
        Boolean aceptaAvisos,
        Integer versionEsperada
) {
    public GuardarPreferenciaUbicacionDTO datos() {
        return new GuardarPreferenciaUbicacionDTO(latitud, longitud, referencia, aceptaAvisos);
    }
}
