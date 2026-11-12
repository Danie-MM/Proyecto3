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

public record CrearMascotaSolicitud(
        @NotBlank(message = "Indicá el nombre de la mascota.")
        @CodePointLength(max = 100, message = "El nombre puede tener hasta 100 caracteres.")
        String nombre,
        @NotNull(message = "Indica la especie de la mascota")
        @Pattern(
                regexp = "[A-Z][A-Z0-9_]{0,29}",
                message = "Usa un codigo de especie valido de hasta 30 caracteres."
        )
        String especie,
        @NotNull(message = "Indicá la edad estimada en meses.")
        @PositiveOrZero(message = "La edad estimada no puede ser negativa.")
        Integer edadEstimadaMeses,
        @NotNull(message = "Indicá el tamaño de la mascota.")
        @Pattern(regexp = "PEQUENO|MEDIANO|GRANDE", message = "Elegí PEQUENO, MEDIANO o GRANDE.")
        String tamano,
        @NotNull(message = "Indicá el nivel de energía de la mascota.")
        @Pattern(regexp = "BAJA|MEDIA|ALTA", message = "Elegí BAJA, MEDIA o ALTA.")
        String energia,
        @NotEmpty(message = "Elegí al menos una etiqueta de comportamiento.")
        @UniqueElements(message = "Cada etiqueta de comportamiento debe aparecer una sola vez.")
        List<@NotNull(message = "Cada etiqueta debe tener un identificador.") UUID> comportamientoIds,
        UUID organizacionId
) {
    // Conserva el constructor individual usado durante los primeros pasos de B-01.
    public CrearMascotaSolicitud(String nombre, String especie, Integer edadEstimadaMeses,
                                String tamano, String energia, List<UUID> comportamientoIds) {
        this(nombre, especie, edadEstimadaMeses, tamano, energia, comportamientoIds, null);
    }

    public CrearMascotaSolicitud {
        if (nombre != null) {
            nombre = nombre.strip();
        }
    }
}
