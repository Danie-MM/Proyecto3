package cr.petmatchcr.mascotas.dto;

import java.util.List;
import java.util.UUID;

public record CatalogosMascotaRespuesta(List<String> especies, List<String> tamanos,
                                       List<String> energias, List<Etiqueta> comportamientos,
                                       String versionCatalogo) {

    public static final List<String> TAMANOS = List.of("PEQUENO", "MEDIANO", "GRANDE");
    public static final List<String> ENERGIAS = List.of("BAJA", "MEDIA", "ALTA");

    public record Etiqueta(UUID id, String nombre, boolean activo) {}
}
