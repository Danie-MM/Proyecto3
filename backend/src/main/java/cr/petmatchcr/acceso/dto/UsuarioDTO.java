
package cr.petmatchcr.acceso.dto;

import cr.petmatchcr.acceso.entities.RolUsuario;

import java.time.LocalDate;

public record UsuarioDTO(
        Long id,
        String nombreCompleto,
        String email,
        LocalDate fechaNacimiento,
        RolUsuario rol,
        boolean activo
) {
}
