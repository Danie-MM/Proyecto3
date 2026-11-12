package cr.petmatchcr.extravios.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PreferenciaUbicacionDTO(
        boolean configurada,
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
    public PreferenciaUbicacionDTO {
        latitud = latitud == null ? null : latitud.stripTrailingZeros();
        longitud = longitud == null ? null : longitud.stripTrailingZeros();
    }

    /** Una cuenta sin preferencias puede seguir usando el resto de la aplicación. */
    public static PreferenciaUbicacionDTO sinConfigurar(UUID usuarioId) {
        return new PreferenciaUbicacionDTO(false, null, usuarioId, null, null, null,
                false, null, null, null, null);
    }
}
