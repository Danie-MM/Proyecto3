
package cr.petmatchcr.acceso.dto;

import cr.petmatchcr.acceso.entities.RolUsuario;

import java.time.LocalDate;
import java.util.UUID;

public record UsuarioDTO(
        Long id,
        UUID idPublico,
        String nombreCompleto,
        String email,
        LocalDate fechaNacimiento,
        RolUsuario rol,
        boolean activo
) {
}
