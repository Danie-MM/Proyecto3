package cr.petmatchcr.mascotas.controller;

import cr.petmatchcr.comun.ErrorApi;
import cr.petmatchcr.comun.FallosPersistencia;
import cr.petmatchcr.mascotas.demo.DemoB01Controller;
import cr.petmatchcr.mascotas.service.ErrorMascota;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

/** Los errores de negocio de B se traducen aquí; común conserva el manejo de infraestructura. */
@Order(-1)
@RestControllerAdvice(assignableTypes = {MascotaController.class, DemoB01Controller.class})
public class MascotaErroresHttp {
    @ExceptionHandler(ErrorMascota.class)
    ResponseEntity<ErrorApi> negocio(ErrorMascota ex) {
        return ResponseEntity.status(ex.getEstado()).body(ErrorApi.crear(ex.getCodigo(), ex.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ErrorApi> persistencia(DataAccessException ex) {
        // No exponemos mensajes del motor: solo códigos conocidos y explicaciones seguras.
        Throwable causa = ex.getMostSpecificCause();
        String estado = causa instanceof SQLException sql && sql.getSQLState() != null ? sql.getSQLState() : "";
        var esperado = switch (estado) {
            case "P0400", "23514", "23503", "22001", "22003", "22021" -> new ErrorMascota(400, "VALIDACION",
                    "Revisá los datos y elegí opciones vigentes de los catálogos.");
            case "P0404" -> new ErrorMascota(404, "NO_ENCONTRADO", "No encontramos esa mascota.");
            case "P0409" -> new ErrorMascota(409, "VERSION_DESACTUALIZADA",
                    "La mascota cambió. Recargá la versión vigente antes de editar.");
            case "P0410" -> new ErrorMascota(409, "CLAVE_REUTILIZADA",
                    "Esa clave de reintento ya se utilizó con otros datos.");
            case "P0411" -> new ErrorMascota(409, "CONFLICTO", "El estado actual no permite esta operación.");
            default -> null;
        };
        if (esperado != null) return negocio(esperado);
        return FallosPersistencia.responder(ex);
    }
}
