package cr.petmatchcr.acceso.api;

import java.util.Optional;
import java.util.UUID;

/** Identidad pública persistente de la cuenta autenticada y todavía activa. */
@FunctionalInterface
public interface IdentidadActual {
    Optional<UUID> usuarioActual();
}
