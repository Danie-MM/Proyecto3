package cr.petmatchcr.mascotas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.validator.constraints.CodePointLength;

/** La baja es lógica: conserva perfil e historial y deja constancia del motivo. */
public record DesactivarMascotaSolicitud(
        @NotBlank(message = "Indicá el motivo de la desactivación.")
        @CodePointLength(max = 500, message = "El motivo puede tener hasta 500 caracteres.") String motivo,
        @NotNull(message = "Indicá la versión que consultaste.")
        @PositiveOrZero(message = "La versión no puede ser negativa.") Integer versionEsperada
) {
    public DesactivarMascotaSolicitud {
        if (motivo != null) motivo = motivo.strip();
    }
}
