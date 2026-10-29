package cr.petmatchcr.entorno;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Expone la prueba técnica únicamente en los entornos local y de pruebas. */
@RestController
@RequestMapping("/api/v1/pruebas-entorno")
@Profile({"local", "test"})
public class PruebaEntornoController {

    private final PruebaEntornoService service;

    /** Recibe las reglas de negocio; el controlador no ejecuta SQL. */
    public PruebaEntornoController(PruebaEntornoService service) {
        this.service = service;
    }

    /** Guarda y devuelve 201 con la ubicación del nuevo registro. */
    @PostMapping
    public ResponseEntity<PruebaEntornoRespuesta> crear(@RequestBody PruebaEntornoSolicitud solicitud) {
        PruebaEntornoRespuesta respuesta = service.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/v1/pruebas-entorno/" + respuesta.id()))
                .body(respuesta);
    }

    /** Devuelve 200 cuando encuentra el UUID solicitado. */
    @GetMapping("/{id}")
    public PruebaEntornoRespuesta consultar(@PathVariable String id) {
        return service.consultar(id);
    }
}
