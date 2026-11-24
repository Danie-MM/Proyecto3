package cr.petmatchcr.mascotas.dto;

import cr.petmatchcr.mascotas.service.ErrorMascota;
import java.util.Set;
import java.util.UUID;

public record FiltrosCatalogoPublico(String nombre, String especie, Integer edadMinimaMeses,
                                   Integer edadMaximaMeses, Set<String> tamanos,
                                   Set<String> energias, Set<UUID> comportamientoIds) {
    public FiltrosCatalogoPublico {
        nombre = nombre == null ? "" : nombre.strip();
        especie = especie == null || especie.isBlank() ? null : especie.strip();
        tamanos = tamanos == null ? Set.of() : Set.copyOf(tamanos);
        energias = energias == null ? Set.of() : Set.copyOf(energias);
        comportamientoIds = comportamientoIds == null ? Set.of() : Set.copyOf(comportamientoIds);
        if (edadMinimaMeses != null && edadMinimaMeses < 0 || edadMaximaMeses != null && edadMaximaMeses < 0
                || edadMinimaMeses != null && edadMaximaMeses != null && edadMinimaMeses > edadMaximaMeses) {
            throw new ErrorMascota(400, "VALIDACION", "Revisá el rango de edad en meses.");
        }
        if (especie != null && !Set.of("PERRO", "GATO").contains(especie)
                || !CatalogosMascotaRespuesta.TAMANOS.containsAll(tamanos)
                || !CatalogosMascotaRespuesta.ENERGIAS.containsAll(energias)) {
            throw new ErrorMascota(400, "VALIDACION", "Elegí opciones válidas del catálogo.");
        }
    }
}
