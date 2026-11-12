package cr.petmatchcr.extravios.dto;

import java.math.BigDecimal;

public record GuardarPreferenciaUbicacionDTO(
        BigDecimal latitud,
        BigDecimal longitud,
        String referencia,
        Boolean aceptaAvisos
) {
    /** Normaliza formato sin convertir una aceptación ausente en false. */
    public GuardarPreferenciaUbicacionDTO {
        latitud = latitud == null ? null : latitud.stripTrailingZeros();
        longitud = longitud == null ? null : longitud.stripTrailingZeros();
        referencia = referencia == null ? null : referencia.strip();
    }
}
