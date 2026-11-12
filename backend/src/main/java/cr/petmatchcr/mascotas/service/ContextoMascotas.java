package cr.petmatchcr.mascotas.service;

import cr.petmatchcr.mascotas.api.AmbitosMascotas;
import cr.petmatchcr.mascotas.api.IdentidadMascotas;
import cr.petmatchcr.mascotas.dto.ContextoMascotasRespuesta;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/** Traduce la identidad autenticada por A a los datos que B necesita para autorizar. */
@Component
public class ContextoMascotas {
    private final AmbitosMascotas ambitos;
    private final IdentidadMascotas identidad;

    public ContextoMascotas(AmbitosMascotas ambitos, IdentidadMascotas identidad) {
        this.ambitos = ambitos;
        this.identidad = identidad;
    }

    public Actor actual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || autenticacion instanceof AnonymousAuthenticationToken) {
            throw new ErrorMascota(401, "NO_AUTENTICADO", "Iniciá sesión para consultar tus mascotas.");
        }
        // A resuelve su principal y revalida la cuenta; no interpretamos nombres/correos como IDs.
        UUID id = identidad.usuarioActual().orElseThrow(() ->
                new ErrorMascota(401, "NO_AUTENTICADO", "La sesión no contiene una identidad vigente para mascotas."));
        return new Actor(id, Set.copyOf(ambitos.organizacionesAdministrables(id)));
    }

    public record Actor(UUID usuarioId, Set<UUID> organizaciones) {}

    /** Sirve tanto al formulario real como a la demostración: usa los mismos permisos del servidor. */
    public ContextoMascotasRespuesta consultar() {
        var actor = actual();
        return new ContextoMascotasRespuesta(actor.usuarioId(), actor.organizaciones().stream().sorted()
                .map(id -> new ContextoMascotasRespuesta.Organizacion(id, ambitos.nombreOrganizacion(id))).toList());
    }
}
