package cr.petmatchcr.extravios.controllers;

import cr.petmatchcr.extravios.dto.ActualizarReporteExtravioDTO;
import cr.petmatchcr.extravios.dto.CrearReporteExtravioDTO;
import cr.petmatchcr.extravios.dto.ReporteExtravioDTO;
import cr.petmatchcr.extravios.dto.ReporteExtravioDetalleDTO;
import cr.petmatchcr.extravios.dto.ReporteExtravioResumenDTO;
import cr.petmatchcr.extravios.service.ReporteExtravioService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Expone las operaciones HTTP relacionadas con
 * la publicación, consulta y actualización
 * de reportes de mascotas extraviadas.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reportes-extravio")
public class ReporteExtravioController {

    private final ReporteExtravioService service;

    // C-02: Publicación de reportes.

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

    // C-03: Consulta y actualización de reportes.

    /**
     * Recupera el listado de reportes activos.
     *
     * @return lista resumida de reportes
     */
    @GetMapping
    public ResponseEntity<List<ReporteExtravioResumenDTO>>
    listarActivos() {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.listarActivos());
    }

    /**
     * Consulta el detalle vigente de un reporte.
     *
     * @param id identificador del reporte
     * @return detalle del reporte solicitado
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReporteExtravioDetalleDTO>
    consultarDetalle(@PathVariable UUID id) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.consultarDetalle(id));
    }

    /**
     * Actualiza los datos permitidos de un reporte
     * mediante una operación autorizada.
     *
     * @param id identificador del reporte
     * @param dto datos de actualización
     * @return detalle actualizado del reporte
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ReporteExtravioDetalleDTO>
    actualizar(
            @PathVariable UUID id,
            @RequestBody ActualizarReporteExtravioDTO dto
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.actualizar(id, dto));
    }
}