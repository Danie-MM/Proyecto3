package cr.petmatchcr.campanias;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface CampaniaRepository extends JpaRepository<Campania, UUID> {
}