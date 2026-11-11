
package cr.petmatchcr.extravios.controllers;

import cr.petmatchcr.extravios.dto.GuardarPreferenciaUbicacionDTO;
import cr.petmatchcr.extravios.dto.PreferenciaUbicacionDTO;
import cr.petmatchcr.extravios.service.PreferenciaUbicacionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/extravios/ubicacion")
@RequiredArgsConstructor
public class PreferenciaUbicacionController {

    private final PreferenciaUbicacionService service;

    @GetMapping
    public ResponseEntity<PreferenciaUbicacionDTO> consultar() {
        return ResponseEntity.ok(service.consultar());
    }

    @PostMapping
    public ResponseEntity<PreferenciaUbicacionDTO> crear(
            @RequestBody GuardarPreferenciaUbicacionDTO dto
    ) {
        return ResponseEntity.status(201).body(service.crear(dto));
    }

    @PutMapping
    public ResponseEntity<PreferenciaUbicacionDTO> actualizar(
            @RequestBody GuardarPreferenciaUbicacionDTO dto
    ) {
        return ResponseEntity.ok(service.actualizar(dto));
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminar() {
        service.eliminar();
        return ResponseEntity.noContent().build();
    }
}
