package cr.petmatchcr.mascotas.controller;

import cr.petmatchcr.mascotas.dto.*;
import cr.petmatchcr.mascotas.service.ErrorMascota;
import cr.petmatchcr.mascotas.service.MascotaService;
import cr.petmatchcr.mascotas.service.ContextoMascotas;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

/** Traduce HTTP al servicio; no decide quién es el dueño ni contiene consultas SQL. */
@RestController
@RequestMapping("/api/v1/mascotas")
public class MascotaController {
    private final MascotaService mascotas;
    private final ContextoMascotas contexto;

    public MascotaController(MascotaService mascotas, ContextoMascotas contexto) {
        this.mascotas = mascotas;
        this.contexto = contexto;
    }

    @GetMapping("/contexto")
    public ResponseEntity<ContextoMascotasRespuesta> contextoActual() { return privado(contexto.consultar()); }

    @GetMapping
    public ResponseEntity<PaginaMascotasRespuesta> listar(
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int limite,
            @RequestParam(required = false) UUID organizacionId, @RequestParam(required = false) Boolean activas) {
        return privado(mascotas.listar(pagina, limite, organizacionId, activas));
    }

    @GetMapping("/{mascotaId}")
    public ResponseEntity<MascotaPrivadaRespuesta> consultar(@PathVariable String mascotaId) {
        return privado(mascotas.consultar(uuid(mascotaId)));
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<MascotaPrivadaRespuesta> crear(@RequestHeader("Idempotency-Key") String clave,
                                                        @Valid @RequestBody CrearMascotaSolicitud solicitud) {
        var creada = mascotas.crear(uuid(clave), solicitud);
        return ResponseEntity.created(URI.create("/api/v1/mascotas/" + creada.id()))
                .cacheControl(CacheControl.noStore()).body(creada);
    }

    @PutMapping(value = "/{mascotaId}", consumes = "application/json")
    public ResponseEntity<MascotaPrivadaRespuesta> editar(@PathVariable String mascotaId,
                                                        @Valid @RequestBody EditarMascotaSolicitud solicitud) {
        return privado(mascotas.editar(uuid(mascotaId), solicitud));
    }

    @PostMapping(value = "/{mascotaId}/desactivacion", consumes = "application/json")
    public ResponseEntity<MascotaPrivadaRespuesta> desactivar(@PathVariable String mascotaId,
            @RequestHeader("Idempotency-Key") String clave, @Valid @RequestBody DesactivarMascotaSolicitud solicitud) {
        return privado(mascotas.desactivar(uuid(mascotaId), uuid(clave), solicitud));
    }

    private static UUID uuid(String valor) {
        try {
            var id = UUID.fromString(valor);
            if (id.toString().equalsIgnoreCase(valor)) return id;
        } catch (IllegalArgumentException ignored) { }
        throw new ErrorMascota(400, "VALIDACION", "Usá un identificador UUID completo y válido.");
    }

    private static <T> ResponseEntity<T> privado(T datos) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(datos);
    }
}
