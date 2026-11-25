package cr.petmatchcr.acceso.dto;

import java.util.UUID;

/**
 * Versión 01
 * Autora: Daniela Morales
 *
 * Descripción: Datos de los miembros de una organización
 * y sus permisos.
 */
public record OrganizacionMiembroResponse(
        UUID usuarioId,
        String nombreCompleto,
        boolean puedeAdministrar,
        boolean activo
) {
}