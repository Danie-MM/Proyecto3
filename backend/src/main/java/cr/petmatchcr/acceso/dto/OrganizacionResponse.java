package cr.petmatchcr.acceso.dto;

import java.util.UUID;

/**
 * Versión 01
 * Autora: Daniela Morales
 *
 * Descripción: Datos de una organización registrada.
 */
public record OrganizacionResponse(
        UUID id,
        String nombre,
        boolean activa
) {
}