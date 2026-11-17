
package cr.petmatchcr.extravios.mappers;

import cr.petmatchcr.extravios.dto.CrearReporteExtravioDTO;
import cr.petmatchcr.extravios.dto.ReporteExtravioDTO;
import cr.petmatchcr.extravios.model.ReporteExtravio;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReporteExtravioMapper {

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

}
