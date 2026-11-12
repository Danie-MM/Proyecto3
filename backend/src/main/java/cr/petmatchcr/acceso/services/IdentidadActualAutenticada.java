package cr.petmatchcr.acceso.services;

import cr.petmatchcr.acceso.api.IdentidadActual;
import cr.petmatchcr.acceso.entities.Usuario;
import cr.petmatchcr.acceso.repositories.UsuarioRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/** Resuelve el correo validado por A sin fabricar UUID a partir de nombres o IDs numéricos. */
@Service
@Profile("!demo-b01")
public class IdentidadActualAutenticada implements IdentidadActual {
    private final UsuarioRepository usuarios;

    public IdentidadActualAutenticada(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Optional<UUID> usuarioActual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || autenticacion instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        // Consultar de nuevo respeta una desactivación posterior a la emisión del JWT.
        return usuarios.findByEmailAndActivoTrue(autenticacion.getName()).map(Usuario::getIdPublico);
    }
}
