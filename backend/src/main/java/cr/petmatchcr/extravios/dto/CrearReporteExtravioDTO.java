
package cr.petmatchcr.extravios.dto;

import cr.petmatchcr.extravios.model.MedioContacto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Datos requeridos para publicar un reporte de extravío.
 */
public record CrearReporteExtravioDTO(

        UUID mascotaId,

        Instant fechaExtravio,

        BigDecimal latitud,

        BigDecimal longitud,

        String descripcion,

        MedioContacto tipoContacto,

        String contacto,

        Boolean autorizaContacto,

        Boolean confirmado

) {}
