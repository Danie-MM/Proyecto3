
package cr.petmatchcr.acceso.repositories;

import cr.petmatchcr.acceso.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
/**
 * Versión 01
 * Autora: Daniela Morales
 *
 * Descripción: Esta interfaz de consultas
 * sobre los usuarios registrados
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por medio del correo
     */

    Optional<Usuario> findByEmail(String email);

    /**
     * Busca un usuario por correo y verifica
     * que la cuenta este activa
     */

    Optional<Usuario> findByEmailAndActivoTrue(String email);
}
