package cr.petmatchcr.mascotas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.UUID;
import org.hibernate.validator.constraints.CodePointLength;
import org.hibernate.validator.constraints.UniqueElements;

/** Campos editables. Propietario, organización y disponibilidad usan otros flujos. */
public record EditarMascotaSolicitud(
        @NotBlank(message = "Indicá el nombre de la mascota.")
        @CodePointLength(max = 100, message = "El nombre puede tener hasta 100 caracteres.") String nombre,
        @NotNull(message = "Indicá la especie de la mascota.")
        @Pattern(regexp = "[A-Z][A-Z0-9_]{0,29}", message = "Elegí una especie del catálogo.") String especie,
        @NotNull(message = "Indicá la edad estimada en meses.")
        @PositiveOrZero(message = "La edad estimada no puede ser negativa.") Integer edadEstimadaMeses,
        @NotNull(message = "Indicá el tamaño de la mascota.")
        @Pattern(regexp = "PEQUENO|MEDIANO|GRANDE", message = "Elegí un tamaño válido.") String tamano,
        @NotNull(message = "Indicá el nivel de energía.")
        @Pattern(regexp = "BAJA|MEDIA|ALTA", message = "Elegí un nivel de energía válido.") String energia,
        @NotEmpty(message = "Elegí al menos una etiqueta de comportamiento.")
        @UniqueElements(message = "Cada etiqueta debe aparecer una sola vez.")
        List<@NotNull(message = "Cada etiqueta debe tener un identificador.") UUID> comportamientoIds,
        @NotNull(message = "Indicá la versión que consultaste.")
        @PositiveOrZero(message = "La versión no puede ser negativa.") Integer versionEsperada
) {
    public EditarMascotaSolicitud {
        if (nombre != null) nombre = nombre.strip();
    }
}
