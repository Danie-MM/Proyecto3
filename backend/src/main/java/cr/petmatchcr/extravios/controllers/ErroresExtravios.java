package cr.petmatchcr.extravios.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice(assignableTypes = PreferenciaUbicacionController.class)
public class ErroresExtravios {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> manejarError(
            ResponseStatusException ex) {

        String codigo = switch (ex.getStatusCode().value()) {
            case 400 -> "SOLICITUD_INVALIDA";
            case 404 -> "PREFERENCIA_NO_ENCONTRADA";
            case 409 -> "PREFERENCIA_YA_EXISTE";
            default -> "ERROR_SOLICITUD";
        };

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(Map.of(
                        "codigo", codigo,
                        "mensaje", ex.getReason() != null
                                ? ex.getReason()
                                : "No se pudo completar la solicitud"
                ));
    }

}
