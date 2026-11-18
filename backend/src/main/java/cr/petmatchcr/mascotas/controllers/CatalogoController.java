package cr.petmatchcr.mascotas.controllers;

import cr.petmatchcr.mascotas.api.MascotasParaAdopciones;
import cr.petmatchcr.mascotas.dto.FiltrosCatalogoPublico;
import cr.petmatchcr.mascotas.dto.PaginaMascotasPublicas;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalogo")
public class CatalogoController {
    private final MascotasParaAdopciones mascotas;

    public CatalogoController(MascotasParaAdopciones mascotas) {
        this.mascotas = mascotas;
    }

    @GetMapping("/mascotas")
    public ResponseEntity<PaginaMascotasPublicas> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String especie,
            @RequestParam(required = false) Integer edadMinimaMeses,
            @RequestParam(required = false) Integer edadMaximaMeses,
            @RequestParam(required = false) Set<String> tamanos,
            @RequestParam(required = false) Set<String> energias,
            @RequestParam(required = false) Set<UUID> comportamientoIds,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int limite) {
        var filtros = new FiltrosCatalogoPublico(nombre, especie, edadMinimaMeses, edadMaximaMeses,
                tamanos, energias, comportamientoIds);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(mascotas.consultarCatalogo(filtros, pagina, limite));
    }
}
