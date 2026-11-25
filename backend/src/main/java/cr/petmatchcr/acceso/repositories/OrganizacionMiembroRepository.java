package cr.petmatchcr.acceso.repositories;

import cr.petmatchcr.acceso.entities.OrganizacionMiembro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Versión 01
 * Autora: Daniela Morales
 *
 * Descripción: Consultas de los miembros asociados
 * a una organización.
 */
public interface OrganizacionMiembroRepository extends JpaRepository<OrganizacionMiembro, Long> {

    Optional<OrganizacionMiembro> findByOrganizacionIdAndUsuarioId(
            UUID organizacionId, Long usuarioId);

    List<OrganizacionMiembro> findByUsuarioIdAndActivoTrueAndOrganizacionActivaTrue(
            Long usuarioId);

    List<OrganizacionMiembro> findByOrganizacionIdAndActivoTrueAndUsuarioActivoTrue(
            UUID organizacionId);
}