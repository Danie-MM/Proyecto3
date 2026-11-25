package cr.petmatchcr.acceso.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Versión 01
 * Autora: Daniela Morales
 *
 * Descripción: Datos necesarios para registrar una organización.
 */
public record OrganizacionRequest(
        @NotBlank String nombre
) {
}