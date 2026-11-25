package cr.petmatchcr.acceso.controllers;

import cr.petmatchcr.acceso.dto.OrganizacionRequest;
import cr.petmatchcr.acceso.dto.OrganizacionResponse;
import cr.petmatchcr.acceso.dto.OrganizacionMiembroResponse;
import cr.petmatchcr.acceso.dto.OrganizacionMiembroRequest;
import cr.petmatchcr.acceso.dto.OrganizacionPermisoRequest;
import cr.petmatchcr.acceso.entities.Organizacion;
import cr.petmatchcr.acceso.services.OrganizacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Versión 01
 * Autora: Daniela Morales
 *
 * Descripción: Controlador para registrar organizaciones
 * y administrar sus miembros.
 */
@RestController
@RequestMapping("/api/v1/organizaciones")
public class OrganizacionController {

    private final OrganizacionService organizacionService;

    public OrganizacionController(OrganizacionService organizacionService) {
        this.organizacionService = organizacionService;
    }

    /**
     * Registra una nueva organización.
     */
    @PostMapping
    public ResponseEntity<?> registrar(@Valid @RequestBody OrganizacionRequest datos) {

        Organizacion organizacion = organizacionService.crear(datos.nombre());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "id", organizacion.getId(),
                        "nombre", organizacion.getNombre(),
                        "activa", organizacion.isActiva()
                )
        );
    }

    /**
     * Agrega un usuario a una organización.
     */
    @PostMapping("/{id}/miembros")
    public ResponseEntity<Void> agregarMiembro(
            @PathVariable UUID id,
            @Valid @RequestBody OrganizacionMiembroRequest datos) {

        organizacionService.agregarMiembro(
                id,
                datos.usuarioId(),
                datos.administrador()
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Cambia el permiso de un miembro.
     */
    @PutMapping("/{id}/miembros/{usuarioId}/permiso")
    public ResponseEntity<Void> cambiarPermiso(
            @PathVariable UUID id,
            @PathVariable UUID usuarioId,
            @Valid @RequestBody OrganizacionPermisoRequest datos) {

        organizacionService.cambiarPermiso(
                id,
                usuarioId,
                datos.administrador()
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Desactiva a un miembro de la organización.
     */
    @DeleteMapping("/{id}/miembros/{usuarioId}")
    public ResponseEntity<Void> quitarMiembro(
            @PathVariable UUID id,
            @PathVariable UUID usuarioId) {

        organizacionService.quitarMiembro(id, usuarioId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Consulta las organizaciones del usuario.
     */
    @GetMapping
    public List<OrganizacionResponse> listarOrganizaciones() {
        return organizacionService.listarOrganizaciones();
    }

    /**
     * Consulta una organización.
     */
    @GetMapping("/{id}")
    public OrganizacionResponse consultarOrganizacion(@PathVariable UUID id) {
        return organizacionService.consultarOrganizacion(id);
    }

    /**
     * Consulta los miembros de una organización.
     */
    @GetMapping("/{id}/miembros")
    public List<OrganizacionMiembroResponse> listarMiembros(@PathVariable UUID id) {
        return organizacionService.listarMiembros(id);
    }
}