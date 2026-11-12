package cr.petmatchcr.comun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;

/** Respuesta segura compartida cuando falla la infraestructura, independiente de cada bloque. */
public final class FallosPersistencia {
    private static final Logger LOG = LoggerFactory.getLogger(FallosPersistencia.class);
    private FallosPersistencia() {}

    public static ResponseEntity<ErrorApi> responder(DataAccessException ex) {
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
