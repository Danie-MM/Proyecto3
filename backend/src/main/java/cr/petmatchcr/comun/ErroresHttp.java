package cr.petmatchcr.comun;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
class ErroresHttp {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorApi> camposInvalidos(MethodArgumentNotValidException ex) {
        var campos = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorApi.Campo(error.getField(), error.getDefaultMessage())).toList();
        return ResponseEntity.badRequest().body(ErrorApi.crear("VALIDACION", "Revisá los campos indicados.",
                campos.toArray(ErrorApi.Campo[]::new)));
    }

    @ExceptionHandler({MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class})
    ResponseEntity<ErrorApi> parametrosInvalidos(Exception ex) {
        return ResponseEntity.badRequest().body(ErrorApi.crear("VALIDACION",
                "Revisá los identificadores, filtros y la clave de reintento enviados."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorApi> formatoNoAdmitido(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(ex.getStatusCode()).headers(ex.getHeaders())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorApi.crear("SOLICITUD_INVALIDA", "Enviá el cuerpo como application/json."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorApi> cuerpoIlegible() {
        return ResponseEntity.badRequest().body(ErrorApi.crear("SOLICITUD_INVALIDA",
                "El cuerpo debe ser un objeto JSON válido.", new ErrorApi.Campo("cuerpo", "Revisá el JSON.")));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ErrorApi> falloDeDatos(DataAccessException ex) {
        return FallosPersistencia.responder(ex);
    }
}
