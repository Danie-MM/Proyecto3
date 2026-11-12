package cr.petmatchcr.mascotas.controller;

import cr.petmatchcr.mascotas.dto.CatalogosMascotaRespuesta;
import cr.petmatchcr.mascotas.repository.CatalogosMascotaRepository;
import cr.petmatchcr.mascotas.model.Especie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static cr.petmatchcr.mascotas.dto.CatalogosMascotaRespuesta.TAMANOS;
import static cr.petmatchcr.mascotas.dto.CatalogosMascotaRespuesta.ENERGIAS;

@RestController
public class CatalogosMascotaController {

    private final CatalogosMascotaRepository repositorio;

    public CatalogosMascotaController(CatalogosMascotaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping("/api/v1/catalogos/mascotas")
    public CatalogosMascotaRespuesta consultar() {
        // El alta ofrece especies vigentes; los perfiles existentes conservan sus referencias históricas.
        var especies = repositorio.listarEspecies().stream().filter(Especie::isActiva)
                .map(Especie::getCodigo).toList();
        return new CatalogosMascotaRespuesta(especies, TAMANOS, ENERGIAS, repositorio.listarComportamientos(), "0.2");
    }
}
