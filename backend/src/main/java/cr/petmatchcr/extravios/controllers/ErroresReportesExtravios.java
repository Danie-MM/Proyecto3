
package cr.petmatchcr.extravios.controllers;

import cr.petmatchcr.comun.ErrorApi;
import cr.petmatchcr.extravios.service.ErrorReporteExtravio;
import cr.petmatchcr.mascotas.service.ErrorMascota;

import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Gestiona los errores controlados de las operaciones
 * relacionadas con reportes de extravío.
 */
@Order(-1)
@RestControllerAdvice(
        assignableTypes = ReporteExtravioController.class
)
public class ErroresReportesExtravios {

    @ExceptionHandler(ErrorReporteExtravio.class)
    public ResponseEntity<ErrorApi> negocio(
            ErrorReporteExtravio error
    ) {
        return ResponseEntity.status(error.getEstado())
                .body(ErrorApi.crear(
                        error.getCodigo(),
                        error.getMessage()
                ));
    }

    /**
     * Gestiona los errores de negocio provenientes del módulo
     * de mascotas durante la publicación de un extravío.
     *
     * @param error excepción generada por el módulo de mascotas
     * @return respuesta HTTP con el error correspondiente
     */
    @ExceptionHandler(ErrorMascota.class)
    public ResponseEntity<ErrorApi> errorMascota(
            ErrorMascota error
    ) {
        return ResponseEntity.status(error.getEstado())
                .body(ErrorApi.crear(
                        error.getCodigo(),
                        error.getMessage()
                ));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorApi> encabezadoAusente(
            MissingRequestHeaderException error
    ) {
        return negocio(new ErrorReporteExtravio(
                400,
                "VALIDACION",
                "La clave de reintento es obligatoria."
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorApi> conflicto(
            DataIntegrityViolationException error
    ) {
        return negocio(new ErrorReporteExtravio(
                409,
                "CONFLICTO",
                "No fue posible completar la publicación."
        ));
    }
}
