package cr.petmatchcr.mascotas.clinica;

import static cr.petmatchcr.mascotas.clinica.DatosClinicos.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ClinicaController {
    private final ServicioClinico clinica;

    ClinicaController(ServicioClinico clinica) { this.clinica = clinica; }

    @PostMapping(value = "/mascotas/{mascotaId}/eventos-veterinarios", consumes = "application/json")
    public ResponseEntity<Evento> registrar(@PathVariable UUID mascotaId, @RequestHeader("Idempotency-Key") UUID clave,
                                             @RequestBody NuevaVacuna entrada) {
        var evento = clinica.registrar(mascotaId, clave, entrada);
        return ResponseEntity.created(URI.create("/api/v1/eventos-veterinarios/" + evento.id()))
                .cacheControl(CacheControl.noStore()).body(evento);
    }

    @GetMapping("/mascotas/{mascotaId}/eventos-veterinarios")
    public ResponseEntity<Pagina<Evento>> eventos(@PathVariable UUID mascotaId, @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta, @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int limite) {
        return privado(clinica.listarEventos(mascotaId, desde, hasta, tipo, pagina, limite));
    }

    @GetMapping("/eventos-veterinarios/{eventoId}")
    public ResponseEntity<Evento> detalle(@PathVariable UUID eventoId) { return privado(clinica.detalle(eventoId)); }

    @GetMapping("/mascotas/{mascotaId}/permisos-clinicos")
    public ResponseEntity<Pagina<Permiso>> permisos(@PathVariable UUID mascotaId,
            @RequestParam(defaultValue = "true") boolean soloActivos,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int limite) {
        return privado(clinica.listarPermisos(mascotaId, soloActivos, pagina, limite));
    }

    @PostMapping(value = "/mascotas/{mascotaId}/permisos-clinicos", consumes = "application/json")
    public ResponseEntity<Permiso> conceder(@PathVariable UUID mascotaId, @RequestHeader("Idempotency-Key") UUID clave,
                                             @RequestBody Concesion solicitud) {
        var permiso = clinica.conceder(mascotaId, clave, solicitud);
        return ResponseEntity.created(URI.create("/api/v1/permisos-clinicos/" + permiso.id()))
                .cacheControl(CacheControl.noStore()).body(permiso);
    }

    @PostMapping(value = "/permisos-clinicos/{permisoId}/revocacion", consumes = "application/json")
    public ResponseEntity<Permiso> revocar(@PathVariable UUID permisoId, @RequestHeader("Idempotency-Key") UUID clave,
                                            @RequestBody Revocacion solicitud) {
        return privado(clinica.revocar(permisoId, clave, solicitud));
    }

    private static <T> ResponseEntity<T> privado(T datos) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(datos);
    }
}
