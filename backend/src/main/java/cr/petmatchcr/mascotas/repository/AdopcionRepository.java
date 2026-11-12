package cr.petmatchcr.mascotas.repository;

import cr.petmatchcr.mascotas.models.Adopcion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdopcionRepository extends JpaRepository<Adopcion, Long> {
}
