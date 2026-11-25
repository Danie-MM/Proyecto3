package cr.petmatchcr.acceso.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Versión 01
 * Autora: Daniela Morales
 *
 * Descripción: Datos para cambiar el permiso de un miembro.
 */
public record OrganizacionPermisoRequest(
        @NotNull Boolean administrador
) {
}