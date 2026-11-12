package cr.petmatchcr.mascotas.dto;

import java.util.List;
import java.util.UUID;

/** Opciones del formulario obtenidas de los permisos vigentes; el navegador no concede ámbitos. */
public record ContextoMascotasRespuesta(UUID usuarioId, List<Organizacion> organizaciones) {
    public record Organizacion(UUID id, String nombre) {}
}
