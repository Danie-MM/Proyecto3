package cr.petmatchcr.comun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

@RestControllerAdvice
class ErroresHttp {
    private static final Logger LOG = LoggerFactory.getLogger(ErroresHttp.class);

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
        Throwable causa = ex.getMostSpecificCause();
        boolean desconectado = ex instanceof DataAccessResourceFailureException
                || (causa instanceof SQLException sql && sql.getSQLState() != null
                    && (sql.getSQLState().startsWith("08") || sql.getSQLState().startsWith("57P0")));
        var error = desconectado
                ? ErrorApi.crear("BD_NO_DISPONIBLE", "La base de datos no está disponible.")
                : ErrorApi.crear("ERROR_INTERNO", "No pudimos completar la solicitud.");
        LOG.error("Fallo de persistencia con traza {}", error.trazaId(), ex);
        return ResponseEntity.status(desconectado ? 503 : 500).body(error);
    }
}
