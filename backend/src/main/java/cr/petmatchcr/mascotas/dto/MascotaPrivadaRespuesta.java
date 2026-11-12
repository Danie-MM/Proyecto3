package cr.petmatchcr.mascotas.dto;

import cr.petmatchcr.mascotas.model.Mascota;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Contrato del perfil privado; nunca se reutiliza como respuesta del catálogo público. */
public record MascotaPrivadaRespuesta(
        UUID id, Instant creadoEn, int version, String nombre, String especie,
        int edadEstimadaMeses, String tamano, String energia, UUID responsableUsuarioId,
        UUID organizacionId, String disponibilidad, boolean activa, List<UUID> comportamientoIds,
        List<Etiqueta> comportamientos, String fotoUrl
) {
    public record Etiqueta(UUID id, String nombre) {}

    public static MascotaPrivadaRespuesta desde(Mascota mascota) {
        return new MascotaPrivadaRespuesta(mascota.getId(), mascota.getCreadoEn(), mascota.getVersion(),
                mascota.getNombre(), mascota.getEspecie().getCodigo(), mascota.getEdadEstimadaMeses(),
                mascota.getTamano(), mascota.getEnergia(), mascota.getResponsableUsuarioId(),
                mascota.getOrganizacionId(), mascota.getDisponibilidad(), mascota.isActiva(),
                mascota.getComportamientoIds(), mascota.getComportamientos().stream()
                    .map(etiqueta -> new Etiqueta(etiqueta.id(), etiqueta.nombre())).toList(), null);
    }
}
