package cr.petmatchcr.mascotas.api;

import java.util.Set;
import java.util.UUID;

/**
 * Punto de integración con A: organizaciones ACTIVAS que el usuario puede administrar ahora.
 * La implementación de A debe consultar permisos vigentes en cada llamada; un rol en el
 * navegador o una lista guardada al iniciar sesión no prueba que el permiso siga vigente.
 */
public interface AmbitosMascotas {
    Set<UUID> organizacionesAdministrables(UUID usuarioId);

    /** A puede aportar el nombre visible. La autorización siempre usa organizacionesAdministrables. */
    default String nombreOrganizacion(UUID organizacionId) { return "Organización " + organizacionId; }
}
