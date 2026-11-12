package cr.petmatchcr.mascotas.repository;

import cr.petmatchcr.mascotas.models.SolicitudAdopcion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoSolicitydAdopcionRepository extends JpaRepository<SolicitudAdopcion, Long> {
}
