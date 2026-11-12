package cr.petmatchcr.mascotas.repository;

import cr.petmatchcr.mascotas.models.SolicitudAdopcion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudAdopcionRepository extends JpaRepository<SolicitudAdopcion, Long> {
}
