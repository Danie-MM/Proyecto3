package cr.petmatchcr.extravios.controllers;

import cr.petmatchcr.comun.ErrorApi;
import cr.petmatchcr.extravios.service.ErrorReporteExtravio;
import cr.petmatchcr.mascotas.service.ErrorMascota;

import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Gestiona los errores controlados de las operaciones
 * relacionadas con reportes de extravío.
 */
@Order(-1)
@RestControllerAdvice(
        assignableTypes = ReporteExtravioController.class
)
public class ErroresReportesExtravios {

    // Errores de negocio del módulo de extravíos.

    @ExceptionHandler(ErrorReporteExtravio.class)
    public ResponseEntity<ErrorApi> negocio(
            ErrorReporteExtravio error
    ) {
        return ResponseEntity.status(error.getEstado())
                .cacheControl(CacheControl.noStore())
                .body(ErrorApi.crear(
                        error.getCodigo(),
                        error.getMessage()
                ));
    }

    /**
     * Gestiona los errores de negocio provenientes
     * del módulo de mascotas.
     *
     * @param error excepción del módulo de mascotas
     * @return respuesta HTTP correspondiente
     */
    @ExceptionHandler(ErrorMascota.class)
    public ResponseEntity<ErrorApi> errorMascota(
            ErrorMascota error
    ) {
        return ResponseEntity.status(error.getEstado())
                .cacheControl(CacheControl.noStore())
                .body(ErrorApi.crear(
                        error.getCodigo(),
                        error.getMessage()
                ));
    }

    // C-02: Publicación e idempotencia.

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
                "No fue posible completar la operación."
        ));
    }

    // C-03: Consulta y actualización.

    /**
     * Gestiona conflictos de concurrencia detectados
     * durante la persistencia de un reporte.
     *
     * @param error excepción de bloqueo optimista
     * @return respuesta HTTP 409
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorApi> conflictoVersion(
            OptimisticLockingFailureException error
    ) {
        return negocio(new ErrorReporteExtravio(
                409,
                "CONFLICTO_VERSION",
                "El reporte fue modificado. "
                        + "Consultá nuevamente su información."
        ));
    }

    /**
     * Gestiona solicitudes con cuerpos JSON inválidos.
     *
     * @param error excepción de lectura del cuerpo
     * @return respuesta HTTP 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApi> cuerpoInvalido(
            HttpMessageNotReadableException error
    ) {
        return negocio(new ErrorReporteExtravio(
                400,
                "VALIDACION",
                "El cuerpo de la solicitud contiene "
                        + "datos inválidos."
        ));
    }

    /**
     * Gestiona parámetros con formatos incompatibles.
     *
     * @param error excepción de conversión
     * @return respuesta HTTP 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorApi> parametroInvalido(
            MethodArgumentTypeMismatchException error
    ) {
        return negocio(new ErrorReporteExtravio(
                400,
                "VALIDACION",
                "El formato de un parámetro "
                        + "de la solicitud es inválido."
        ));
    }
}