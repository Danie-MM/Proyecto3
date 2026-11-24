package cr.petmatchcr.extravios.dto;

import cr.petmatchcr.extravios.model.EstadoReporteExtravio;
import cr.petmatchcr.extravios.model.MedioContacto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Información permitida para consultar el detalle
 * vigente de un reporte de extravío.
 */
public record ReporteExtravioDetalleDTO(

        UUID id,

        UUID mascotaId,

        String nombreMascota,

        String especie,

        Instant fechaExtravio,

        BigDecimal latitud,

        BigDecimal longitud,

        String descripcion,

        MedioContacto tipoContacto,

        String contacto,

        EstadoReporteExtravio estado,

        Instant creadoEn,

        Instant actualizadoEn,

        Instant cerradoEn,

        boolean puedeEditar,

        Integer version

) {}