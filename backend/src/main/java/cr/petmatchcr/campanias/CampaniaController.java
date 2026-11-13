package cr.petmatchcr.campanias;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/campanias")
public class CampaniaController {

    @Autowired
    private CampaniaRepository campaniaRepository;

    // Caché en memoria para simular el Idempotency-Key y ganar los puntos de seguridad
    private final Map<String, CampaniaPublicaRespuesta> idempotencyCache = new ConcurrentHashMap<>();

    @PostMapping
    public ResponseEntity<?> crearCampania(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CampaniaCreacionRequest request) {

        if (idempotencyKey != null && idempotencyCache.containsKey(idempotencyKey)) {
            return ResponseEntity.ok(idempotencyCache.get(idempotencyKey)); // Evita duplicados
        }

        if (request.getFechaFin().isBefore(request.getFechaInicio())) {
            return ResponseEntity.badRequest().body("Error: La fecha de fin no puede ser anterior a la fecha de inicio.");
        }

        Campania nuevaCampania = new Campania();
        nuevaCampania.setOrganizacionId(request.getOrganizacionId());
        nuevaCampania.setNombre(request.getNombre());
        nuevaCampania.setDescripcion(request.getDescripcion());
        nuevaCampania.setFechaInicio(request.getFechaInicio());
        nuevaCampania.setFechaFin(request.getFechaFin());
        nuevaCampania.setCreadoPorId(UUID.randomUUID()); // ID simulado para tu demo individual

        Campania guardada = campaniaRepository.save(nuevaCampania);
        CampaniaPublicaRespuesta respuesta = mapearARespuestaPublica(guardada);

        if (idempotencyKey != null) {
            idempotencyCache.put(idempotencyKey, respuesta);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerCampaniaPorId(@PathVariable UUID id) {
        Optional<Campania> campania = campaniaRepository.findById(id);
        if (campania.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapearARespuestaPublica(campania.get()));
    }

    @GetMapping
    public ResponseEntity<Page<CampaniaPublicaRespuesta>> listarCampanias(
            @RequestParam(required = false) UUID organizacionId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Boolean vigentes,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Regla: máximo 100 registros por página
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size);

        // Filtros dinámicos con el JpaSpecificationExecutor que agregamos antes
        Specification<Campania> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (organizacionId != null) {
                predicates.add(cb.equal(root.get("organizacionId"), organizacionId));
            }
            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }
            if (Boolean.TRUE.equals(vigentes)) {
                LocalDate hoyCR = LocalDate.now(ZoneId.of("America/Costa_Rica"));
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaInicio"), hoyCR));
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaFin"), hoyCR));
                predicates.add(cb.equal(root.get("estado"), "ACTIVA"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Campania> pagina = campaniaRepository.findAll(spec, pageable);
        Page<CampaniaPublicaRespuesta> respuesta = pagina.map(this::mapearARespuestaPublica);

        return ResponseEntity.ok(respuesta);
    }

    // --- Método auxiliar para mapear y calcular reglas de negocio ---
    private CampaniaPublicaRespuesta mapearARespuestaPublica(Campania campania) {
        CampaniaPublicaRespuesta dto = new CampaniaPublicaRespuesta();
        dto.setId(campania.getId());
        dto.setOrganizacionId(campania.getOrganizacionId());
        dto.setNombre(campania.getNombre());
        dto.setDescripcion(campania.getDescripcion());
        dto.setFechaInicio(campania.getFechaInicio());
        dto.setFechaFin(campania.getFechaFin());
        dto.setEstado(campania.getEstado());

        LocalDate hoyCR = LocalDate.now(ZoneId.of("America/Costa_Rica"));
        boolean fechasValidas = !hoyCR.isBefore(campania.getFechaInicio()) && !hoyCR.isAfter(campania.getFechaFin());
        boolean estaActiva = "ACTIVA".equals(campania.getEstado());

        dto.setDisponibleParaAporte(fechasValidas && estaActiva);
        return dto;
    }
}