
package cr.petmatchcr.extravios.dto;

import cr.petmatchcr.extravios.model.EstadoReporteExtravio;
import cr.petmatchcr.extravios.model.MedioContacto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta de un reporte de extravío
 * para operaciones autorizadas.
 */
public record ReporteExtravioDTO(

        UUID id,

        UUID mascotaId,

        UUID creadoPorUsuarioId,

        Instant fechaExtravio,

        BigDecimal latitud,

        BigDecimal longitud,

        Integer radioMetros,

        String descripcion,

        MedioContacto tipoContacto,

        String contacto,

        Instant contactoAutorizadoEn,

        EstadoReporteExtravio estado,

        Instant actualizadoEn,

        Instant creadoEn,

        Integer version

) {}
