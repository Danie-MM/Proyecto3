package cr.petmatchcr.mascotas.clinica;

import cr.petmatchcr.comun.ErrorApi;
import cr.petmatchcr.comun.FallosPersistencia;
import cr.petmatchcr.mascotas.service.ErrorMascota;
import java.sql.SQLException;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(-1)
@RestControllerAdvice(assignableTypes = ClinicaController.class)
class ErroresClinicos {
    @ExceptionHandler(ErrorMascota.class)
    ResponseEntity<ErrorApi> negocio(ErrorMascota error) {
        return ResponseEntity.status(error.getEstado()).body(ErrorApi.crear(error.getCodigo(), error.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ErrorApi> persistencia(DataAccessException error) {
        if (error.getMostSpecificCause() instanceof SQLException sql && sql.getSQLState() != null) {
            var negocio = switch (sql.getSQLState()) {
                case "P0400", "23514" -> new ErrorMascota(400, "VALIDACION", "Revisá los datos ingresados.");
                case "P0409" -> new ErrorMascota(409, "VERSION_DESACTUALIZADA", "El permiso cambió. Recargá su estado.");
                case "P0410" -> new ErrorMascota(409, "CLAVE_REUTILIZADA", "La clave de reenvío corresponde a otros datos.");
                case "P0411" -> new ErrorMascota(409, "CONFLICTO", "Ya existe un permiso vigente para ese profesional.");
                default -> null;
            };
            if (negocio != null) return negocio(negocio);
        }
        return FallosPersistencia.responder(error);
    }
}
