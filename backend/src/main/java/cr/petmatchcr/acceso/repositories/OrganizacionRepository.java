package cr.petmatchcr.acceso.repositories;

import cr.petmatchcr.acceso.entities.Organizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Versión: 1.0
 * Autora: Daniela Morales
 * Descripción: Permite guardar y consultar organizaciones.
 */
public interface OrganizacionRepository
        extends JpaRepository<Organizacion, UUID> {
}
