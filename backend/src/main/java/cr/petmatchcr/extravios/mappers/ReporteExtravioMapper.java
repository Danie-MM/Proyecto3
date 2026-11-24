package cr.petmatchcr.extravios.mappers;

import cr.petmatchcr.extravios.dto.CrearReporteExtravioDTO;
import cr.petmatchcr.extravios.dto.ReporteExtravioDTO;
import cr.petmatchcr.extravios.dto.ReporteExtravioDetalleDTO;
import cr.petmatchcr.extravios.dto.ReporteExtravioResumenDTO;
import cr.petmatchcr.extravios.model.ReporteExtravio;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Convierte entidades y DTO asociados a los
 * reportes de mascotas extraviadas.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReporteExtravioMapper {

    // C-02: Creación de reportes.

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creadoPorUsuarioId", ignore = true)
    @Mapping(target = "latitudOrigenAviso", ignore = true)
    @Mapping(target = "longitudOrigenAviso", ignore = true)
    @Mapping(target = "radioMetros", ignore = true)
    @Mapping(target = "contactoAutorizadoEn", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "actualizadoEn", ignore = true)
    @Mapping(target = "cerradoEn", ignore = true)
    @Mapping(target = "cerradoPorUsuarioId", ignore = true)
    @Mapping(target = "motivoCierre", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "version", ignore = true)
    ReporteExtravio toEntity(CrearReporteExtravioDTO dto);

    ReporteExtravioDTO toDTO(ReporteExtravio entidad);

    // C-03: Consulta de reportes.

    /**
     * Convierte un reporte en su representación resumida.
     *
     * @param entidad reporte de extravío
     * @param nombreMascota nombre público de la mascota
     * @param especie especie de la mascota
     * @return resumen del reporte
     */
    @Mapping(target = "nombreMascota", source = "nombreMascota")
    @Mapping(target = "especie", source = "especie")
    ReporteExtravioResumenDTO toResumenDTO(
            ReporteExtravio entidad,
            String nombreMascota,
            String especie
    );

    /**
     * Convierte un reporte en su representación detallada.
     *
     * @param entidad reporte de extravío
     * @param nombreMascota nombre público de la mascota
     * @param especie especie de la mascota
     * @param puedeEditar autorización calculada por el servicio
     * @return detalle del reporte
     */
    @Mapping(target = "nombreMascota", source = "nombreMascota")
    @Mapping(target = "especie", source = "especie")
    @Mapping(target = "puedeEditar", source = "puedeEditar")
    ReporteExtravioDetalleDTO toDetalleDTO(
            ReporteExtravio entidad,
            String nombreMascota,
            String especie,
            boolean puedeEditar
    );
}