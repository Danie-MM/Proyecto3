package cr.petmatchcr.mascotas.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de integración con A. Entrega el ID acordado de la cuenta autenticada y vigente.
 * Una sesión ausente, revocada o no vinculada devuelve vacío. El adaptador de A debe
 * resolver su identidad real; B no convierte correos ni números en UUID artificiales.
 */
@FunctionalInterface
public interface IdentidadMascotas {
    Optional<UUID> usuarioActual();
}
