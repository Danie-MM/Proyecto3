package cr.petmatchcr.mascotas.publicacion;

import cr.petmatchcr.mascotas.dto.MascotaPrivadaRespuesta;
import cr.petmatchcr.mascotas.dto.MascotaPublicaRespuesta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import cr.petmatchcr.comun.ErrorApi;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mascotas/{mascotaId}")
public class PublicacionController {
    private final PublicacionService publicacion;

    public PublicacionController(PublicacionService publicacion) { this.publicacion = publicacion; }

    public record Disponibilidad(@NotNull String disponibilidad, @NotNull @PositiveOrZero Integer versionEsperada) {}
    public record Retiro(@NotNull @PositiveOrZero Integer versionEsperada) {}

    @PutMapping(value="/disponibilidad", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MascotaPrivadaRespuesta> disponibilidad(@PathVariable UUID mascotaId,
            @Valid @RequestBody Disponibilidad solicitud) {
        return respuesta(publicacion.disponibilidad(mascotaId, solicitud.versionEsperada(), solicitud.disponibilidad()));
    }

    @GetMapping("/publica")
    public ResponseEntity<MascotaPublicaRespuesta> perfil(@PathVariable UUID mascotaId) {
        return respuesta(publicacion.consultarPerfilPublico(mascotaId));
    }

    @PostMapping(value="/foto", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PublicacionService.FotoGuardada> cargar(@PathVariable UUID mascotaId,
            @RequestHeader("Idempotency-Key") UUID clave, @RequestParam @PositiveOrZero int versionEsperada,
            @RequestParam MultipartFile archivo) {
        var foto = publicacion.cargar(mascotaId, clave, versionEsperada, archivo);
        return ResponseEntity.created(URI.create(foto.urlPrivada())).cacheControl(CacheControl.noStore()).body(foto);
    }

    @PostMapping(value="/foto/retiro", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PublicacionService.FotoRetirada> retirar(@PathVariable UUID mascotaId,
            @RequestHeader("Idempotency-Key") UUID clave, @Valid @RequestBody Retiro solicitud) {
        return respuesta(publicacion.retirar(mascotaId, clave, solicitud.versionEsperada()));
    }

    @GetMapping("/foto")
    public ResponseEntity<byte[]> fotoPrivada(@PathVariable UUID mascotaId) {
        return imagen(publicacion.fotoPrivada(mascotaId));
    }

    @GetMapping("/foto/publica")
    public ResponseEntity<byte[]> fotoPublica(@PathVariable UUID mascotaId) {
        return imagen(publicacion.fotoPublica(mascotaId));
    }

    private static ResponseEntity<byte[]> imagen(PublicacionRepository.Foto foto) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(foto.mime())).body(foto.bytes());
    }

    private static <T> ResponseEntity<T> respuesta(T valor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(valor);
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class,
            HandlerMethodValidationException.class})
    ResponseEntity<ErrorApi> cargaIncompleta() {
        return ResponseEntity.badRequest().body(ErrorApi.crear("VALIDACION",
                "Indicá la fotografía y una versión válida del perfil."));
    }
}
