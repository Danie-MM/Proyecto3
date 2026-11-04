package cr.petmatchcr.campanias;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/campanias")
public class CampaniaController {

    @Autowired
    private CampaniaRepository campaniaRepository;

    @GetMapping
    public List<Campania> listarTodas() {
        return campaniaRepository.findAll();
    }

    @PostMapping
    public Campania guardar(@RequestBody Campania campania) {
        return campaniaRepository.save(campania);
    }
}