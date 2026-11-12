package cr.petmatchcr.extravios.controllers;

import cr.petmatchcr.comun.ErrorApi;
import cr.petmatchcr.extravios.service.ErrorUbicacion;
import jakarta.persistence.OptimisticLockException;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Devuelve errores del contrato sin publicar SQL ni detalles internos de persistencia. */
@Order(-1)
@RestControllerAdvice(assignableTypes = PreferenciaUbicacionController.class)
public class ErroresExtravios {
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorApi> parametroAusente(MissingServletRequestParameterException error) {
        return negocio(new ErrorUbicacion(400, "VALIDACION", "Indicá la versión de la última consulta."));
    }

    @ExceptionHandler(ErrorUbicacion.class)
    public ResponseEntity<ErrorApi> negocio(ErrorUbicacion error) {
        return ResponseEntity.status(error.getEstado()).body(ErrorApi.crear(error.getCodigo(), error.getMessage()));
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorApi> version(Exception error) {
        return negocio(new ErrorUbicacion(409, "VERSION_DESACTUALIZADA",
                "La ubicación cambió. Recargá antes de volver a guardar."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorApi> duplicado(DataIntegrityViolationException error) {
        return negocio(new ErrorUbicacion(409, "CONFLICTO",
                "El estado de la ubicación cambió. Volvé a consultar antes de guardar."));
    }
}
