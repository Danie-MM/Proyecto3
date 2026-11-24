package cr.petmatchcr.extravios.dto;

import cr.petmatchcr.extravios.model.EstadoReporteExtravio;

import java.time.Instant;
import java.util.UUID;

/**
 * Información resumida de un reporte de extravío
 * para su presentación en el listado de reportes.
 */
public record ReporteExtravioResumenDTO(

        UUID id,

        UUID mascotaId,

        String nombreMascota,

        String especie,

        Instant fechaExtravio,

        EstadoReporteExtravio estado,

        Instant actualizadoEn

) {}