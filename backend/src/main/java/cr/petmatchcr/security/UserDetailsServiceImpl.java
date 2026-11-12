
package cr.petmatchcr.security;

import cr.petmatchcr.acceso.entities.Usuario;
import cr.petmatchcr.acceso.repositories.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Versión 01
 * Autora: Daniela Morales
 *
 * Descripción: La clase consulta los usuarios activos
 *              y obtiene sus datos y roles para Spring Security.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Busca un usuario activo por email.
     *
     * Recibe email  del usuario.
     * Devuelve Ddatos del usuario para Spring Security.
     *
     * @throws UsernameNotFoundException en caso de que
     * el usuario no existe o está inactivo.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + email
                ));

        String rol = usuario.getRol().name();

        SimpleGrantedAuthority autoridad = new SimpleGrantedAuthority("ROLE_" + rol);

        return new User(
                usuario.getEmail(),
                "",
                List.of(autoridad)
        );
    }
}
