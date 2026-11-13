package cr.petmatchcr.mascotas.controllers;

import cr.petmatchcr.mascotas.models.VersionCompatibilidad;
import cr.petmatchcr.mascotas.service.CatalogoService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogo")
@Slf4j
public class CatalogoController {

   /* private final CatalogoService catalogoService;
    private final CatalogoMapper catalogoMapper;

    public CatalogoController (CatalogoService catalogoService, CatalogoMapper catalogoMapper) {
        this.catalogoService = catalogoService;
        this.catalogoMapper = catalogoMapper;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<VersionCompatibilidad> listarDisponibles() {
        log.info("Listando todas las mascotas en el catálogo.");
        return catalogoMapper.toDTOList(catalogoService.listarMascotasDisponibles());
    }

    */
}
