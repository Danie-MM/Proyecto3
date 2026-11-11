package cr.petmatchcr.extravios.repository;

import cr.petmatchcr.extravios.model.PreferenciaUbicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreferenciaUbicacionRepository
        extends JpaRepository<PreferenciaUbicacion, UUID> {

    Optional<PreferenciaUbicacion> findByUsuarioId(UUID usuarioId);

}