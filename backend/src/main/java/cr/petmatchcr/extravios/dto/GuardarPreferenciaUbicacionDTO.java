package cr.petmatchcr.extravios.dto;

import java.math.BigDecimal;

public record GuardarPreferenciaUbicacionDTO(
        BigDecimal latitud,
        BigDecimal longitud,
        String referencia,
        boolean aceptaAvisos
) {
}