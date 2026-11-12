package cr.petmatchcr.mascotas.model;

import java.util.UUID;

/** Una etiqueta del catálogo; su vigencia no elimina la historia de la mascota. */
public record Comportamiento(UUID id, String nombre, boolean activa) {}
