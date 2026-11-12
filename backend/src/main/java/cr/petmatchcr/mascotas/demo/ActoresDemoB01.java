package cr.petmatchcr.mascotas.demo;

import cr.petmatchcr.acceso.api.IdentidadActual;
import cr.petmatchcr.mascotas.api.AmbitosMascotas;
import cr.petmatchcr.mascotas.api.IdentidadMascotas;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Identidades ficticias EXCLUSIVAS de la demostración. A proveerá usuarios y permisos reales. */
@Component
@Primary
@Profile("local & demo-b01")
public class ActoresDemoB01 implements AmbitosMascotas, IdentidadMascotas, IdentidadActual {
    private static final List<ActorDemo> ACTORES = List.of(
            new ActorDemo(UUID.fromString("11111111-1111-4111-8111-111111111111"), "Responsable de Luna", List.of()),
            new ActorDemo(UUID.fromString("22222222-2222-4222-8222-222222222222"), "Otro responsable", List.of()),
            new ActorDemo(UUID.fromString("33333333-3333-4333-8333-333333333333"), "Representante del Refugio A",
                    List.of(new OrganizacionDemo(UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"), "Refugio A (demo)"))),
            new ActorDemo(UUID.fromString("44444444-4444-4444-8444-444444444444"), "Representante del Refugio B",
                    List.of(new OrganizacionDemo(UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"), "Refugio B (demo)")))
    );

    public List<ActorDemo> listar() { return ACTORES; }

    public boolean existe(UUID actorId) { return ACTORES.stream().anyMatch(actor -> actor.id().equals(actorId)); }

    /** Solo reconoce los actores ficticios que esta demostración permite seleccionar. */
    @Override
    public Optional<UUID> usuarioActual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || autenticacion instanceof AnonymousAuthenticationToken) return Optional.empty();
        try {
            var id = UUID.fromString(autenticacion.getName());
            return existe(id) && id.toString().equalsIgnoreCase(autenticacion.getName())
                    ? Optional.of(id) : Optional.empty();
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Set<UUID> organizacionesAdministrables(UUID usuarioId) {
        return ACTORES.stream().filter(actor -> actor.id().equals(usuarioId))
                .flatMap(actor -> actor.organizaciones().stream()).map(OrganizacionDemo::id).collect(Collectors.toSet());
    }

    @Override
    public String nombreOrganizacion(UUID organizacionId) {
        return ACTORES.stream().flatMap(actor -> actor.organizaciones().stream())
                .filter(org -> org.id().equals(organizacionId)).map(OrganizacionDemo::nombre).findFirst()
                .orElseGet(() -> AmbitosMascotas.super.nombreOrganizacion(organizacionId));
    }

    public record ActorDemo(UUID id, String nombre, List<OrganizacionDemo> organizaciones) {}
    public record OrganizacionDemo(UUID id, String nombre) {}
}
