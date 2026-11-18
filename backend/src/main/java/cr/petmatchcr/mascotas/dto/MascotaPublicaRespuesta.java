package cr.petmatchcr.mascotas.dto;

import java.util.List;
import java.util.UUID;

public record MascotaPublicaRespuesta(
        UUID id, String nombre, String especie, int edadEstimadaMeses,
        String tamano, String energia, List<Etiqueta> comportamientos,
        String fotoUrl, Organizacion organizacion, String disponibilidad) {
    public record Organizacion(UUID id, String nombre) {}
    public record Etiqueta(UUID id, String nombre) {}
}
