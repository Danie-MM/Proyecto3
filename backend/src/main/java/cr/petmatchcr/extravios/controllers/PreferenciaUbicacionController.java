package cr.petmatchcr.extravios.controllers;

import cr.petmatchcr.extravios.dto.ActualizarPreferenciaUbicacionDTO;
import cr.petmatchcr.extravios.dto.GuardarPreferenciaUbicacionDTO;
import cr.petmatchcr.extravios.dto.PreferenciaUbicacionDTO;
import cr.petmatchcr.extravios.service.PreferenciaUbicacionService;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** La ruta anterior se conserva mientras los consumidores adoptan el contrato de C-01. */
@RestController
@RequestMapping({"/api/v1/mi-ubicacion", "/api/extravios/ubicacion"})
@RequiredArgsConstructor
public class PreferenciaUbicacionController {
    private final PreferenciaUbicacionService service;

    @GetMapping
    public ResponseEntity<PreferenciaUbicacionDTO> consultar() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.consultar());
    }

    @PostMapping
    public ResponseEntity<PreferenciaUbicacionDTO> crear(
            @RequestBody GuardarPreferenciaUbicacionDTO dto,
            @RequestHeader("Idempotency-Key") UUID clave) {
        return ResponseEntity.created(URI.create("/api/v1/mi-ubicacion"))
                .cacheControl(CacheControl.noStore()).body(service.crear(dto, clave));
    }

    @PutMapping
    public ResponseEntity<PreferenciaUbicacionDTO> actualizar(@RequestBody ActualizarPreferenciaUbicacionDTO dto) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.actualizar(dto));
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminar(@RequestParam Integer versionEsperada) {
        service.eliminar(versionEsperada);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
