package cr.petmatchcr.mascotas.api;

import java.util.UUID;

/** Datos mínimos que C puede usar después de comprobar el acceso privado. */
public record ResumenMascotaExtravio(UUID mascotaId, String nombre, String especie) {}
