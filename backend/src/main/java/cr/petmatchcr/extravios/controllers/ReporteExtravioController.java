
package cr.petmatchcr.extravios.controllers;

import cr.petmatchcr.extravios.dto.CrearReporteExtravioDTO;
import cr.petmatchcr.extravios.dto.ReporteExtravioDTO;
import cr.petmatchcr.extravios.service.ReporteExtravioService;

import java.net.URI;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Expone las operaciones HTTP relacionadas con
 * la publicación de reportes de mascotas extraviadas.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reportes-extravio")
public class ReporteExtravioController {

    private final ReporteExtravioService service;

    /**
     * Publica un reporte de extravío confirmado.
     *
     * @param dto datos de la publicación
     * @param clave clave de idempotencia
     * @return reporte publicado o recuperado
     */
    @PostMapping
    public ResponseEntity<ReporteExtravioDTO> crear(
            @RequestBody CrearReporteExtravioDTO dto,
            @RequestHeader("Idempotency-Key") UUID clave
    ) {
        var resultado = service.crear(dto, clave);

        if (resultado.estadoHttp() == 201) {
            return ResponseEntity.created(
                            URI.create("/api/v1/reportes-extravio/"
                                    + resultado.reporte().id())
                    )
                    .cacheControl(CacheControl.noStore())
                    .body(resultado.reporte());
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(resultado.reporte());
    }
}
