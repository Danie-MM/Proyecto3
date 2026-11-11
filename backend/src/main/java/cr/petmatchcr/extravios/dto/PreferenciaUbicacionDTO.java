package cr.petmatchcr.extravios.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PreferenciaUbicacionDTO(
        UUID id,
        UUID usuarioId,
        BigDecimal latitud,
        BigDecimal longitud,
        String referencia,
        boolean aceptaAvisos,
        Instant consentimientoEn,
        Instant actualizadaEn,
        Instant creadoEn,
        Integer version
) {
}
