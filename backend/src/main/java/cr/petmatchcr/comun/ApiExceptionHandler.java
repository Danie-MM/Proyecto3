package cr.petmatchcr.comun;

import cr.petmatchcr.entorno.PruebaEntornoNoEncontradaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientConnectionException;
import java.util.List;

/** Traduce los fallos a respuestas comprensibles, sin enviar SQL ni contraseñas. */
@RestControllerAdvice(basePackages = "cr.petmatchcr")
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Informa errores de texto o UUID sin guardar datos inválidos. */
    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<ErrorRespuesta> validacion(ValidacionException exception) {
        return ResponseEntity.badRequest().body(ErrorRespuesta.crear("VALIDACION",
                "Revisá los datos enviados.", List.of(exception.getErrorCampo())));
    }

    /** Rechaza JSON incompleto, tipos incorrectos y propiedades fuera del contrato. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> jsonInvalido(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ErrorRespuesta.crear("SOLICITUD_INVALIDA",
                "El cuerpo debe ser un objeto JSON con únicamente el campo texto de tipo string.",
                List.of(new ErrorCampo("cuerpo", "Revisá el formato JSON y el campo texto."))));
    }

    /** Distingue un registro inexistente de una base de datos caída. */
    @ExceptionHandler(PruebaEntornoNoEncontradaException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(PruebaEntornoNoEncontradaException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorRespuesta.crear(
                "NO_ENCONTRADO", exception.getMessage(), List.of()));
    }

    /** Usa 503 para fallos de conexión; los errores de programación conservan 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception exception) {
        if (exception instanceof org.springframework.web.ErrorResponse error
                && error.getStatusCode().is4xxClientError()) {
            String mensaje = switch (error.getStatusCode().value()) {
                case 405 -> "El método HTTP no está permitido para esta ruta.";
                case 406 -> "No podemos responder en el formato solicitado.";
                case 415 -> "El contenido debe enviarse como application/json.";
                default -> "Revisá la solicitud enviada.";
            };
            return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders())
                    .body(ErrorRespuesta.crear("SOLICITUD_INVALIDA", mensaje, List.of()));
        }
        boolean conexion = esFalloDeConexion(exception);
        ErrorRespuesta respuesta = ErrorRespuesta.crear(
                conexion ? "BD_NO_DISPONIBLE" : "ERROR_INTERNO",
                conexion ? "La base de datos no está disponible. Intentá de nuevo."
                        : "No pudimos completar la solicitud.",
                List.of());
        LOGGER.error("Fallo del backend [{}]", respuesta.trazaId(), exception);
        return ResponseEntity.status(conexion ? HttpStatus.SERVICE_UNAVAILABLE
                : HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
    }

    private boolean esFalloDeConexion(Throwable exception) {
        Throwable actual = exception;
        for (int profundidad = 0; actual != null && profundidad < 20; profundidad++) {
            if (actual instanceof DataAccessResourceFailureException
                    || actual instanceof SQLTransientConnectionException
                    || actual instanceof SQLNonTransientConnectionException) {
                return true;
            }
            if (actual instanceof SQLException sqlException && sqlException.getSQLState() != null) {
                String estado = sqlException.getSQLState();
                if (estado.startsWith("08") || List.of("57P01", "57P02", "57P03").contains(estado)) {
                    return true;
                }
            }
            actual = actual.getCause();
        }
        return false;
    }
}
