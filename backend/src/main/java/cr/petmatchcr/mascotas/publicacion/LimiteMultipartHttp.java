package cr.petmatchcr.mascotas.publicacion;

import cr.petmatchcr.comun.ErrorApi;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Multipart se analiza antes de que MVC pueda seleccionar el controlador de la ruta. */
@Order(-1)
@RestControllerAdvice
class LimiteMultipartHttp {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorApi> archivoExcesivo() {
        return ResponseEntity.status(413).body(ErrorApi.crear("FOTO_DEMASIADO_GRANDE",
                "La fotografía supera 5 000 000 bytes."));
    }
}
