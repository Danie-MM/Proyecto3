package cr.petmatchcr.extravios.repository;

import cr.petmatchcr.extravios.model.EstadoReporteExtravio;
import cr.petmatchcr.extravios.model.ReporteExtravio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la persistencia y consulta
 * de reportes de mascotas extraviadas.
 */
public interface ReporteExtravioRepository
        extends JpaRepository<ReporteExtravio, UUID> {

    /**
     * Comprueba si existe un reporte en el estado indicado.
     *
     * @param mascotaId identificador de la mascota
     * @param estado estado del reporte
     * @return true si existe un reporte coincidente
     */
    boolean existsByMascotaIdAndEstado(
            UUID mascotaId,
            EstadoReporteExtravio estado
    );

    /**
     * Recupera el reporte de una mascota según su estado.
     *
     * @param mascotaId identificador de la mascota
     * @param estado estado del reporte
     * @return reporte encontrado, si existe
     */
    Optional<ReporteExtravio> findByMascotaIdAndEstado(
            UUID mascotaId,
            EstadoReporteExtravio estado
    );

    /**
     * Recupera los reportes según su estado, ordenados
     * por fecha de extravío de forma descendente.
     *
     * @param estado estado de los reportes solicitados
     * @return lista de reportes coincidentes
     */
    List<ReporteExtravio> findByEstadoOrderByFechaExtravioDesc(
            EstadoReporteExtravio estado
    );
}