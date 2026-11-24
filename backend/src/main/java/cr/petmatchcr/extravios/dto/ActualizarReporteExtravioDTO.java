package cr.petmatchcr.extravios.dto;

import cr.petmatchcr.extravios.model.MedioContacto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Datos permitidos para corregir un reporte de extravío
 * mediante una operación autorizada.
 *
 * No permite modificar el origen histórico de los avisos
 * ni el estado del reporte.
 */
public record ActualizarReporteExtravioDTO(

        Instant fechaExtravio,

        BigDecimal latitud,

        BigDecimal longitud,

        String descripcion,

        MedioContacto tipoContacto,

        String contacto,

        Boolean autorizaContacto,

        String motivoAdministrativo,

        Integer version

) {}