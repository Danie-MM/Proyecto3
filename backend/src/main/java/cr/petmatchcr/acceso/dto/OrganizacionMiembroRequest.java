package cr.petmatchcr.acceso.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Versión 01
 * Autora: Daniela Morales
 *
 * Descripción: Datos de un usuario que se agrega a una organización.
 */
public record OrganizacionMiembroRequest(
        @NotNull UUID usuarioId,
        @NotNull Boolean administrador
) {
}