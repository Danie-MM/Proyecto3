package cr.petmatchcr.extravios.mappers;
import cr.petmatchcr.extravios.dto.GuardarPreferenciaUbicacionDTO;
import cr.petmatchcr.extravios.dto.PreferenciaUbicacionDTO;
import cr.petmatchcr.extravios.model.PreferenciaUbicacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;


@Mapper(componentModel = "spring")
public interface PreferenciaUbicacionMapper {

    @Mapping(target = "configurada", constant = "true")
    PreferenciaUbicacionDTO toDTO(
            PreferenciaUbicacion entidad
    );

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuarioId", ignore = true)
    @Mapping(target = "consentimientoEn", ignore = true)
    @Mapping(target = "actualizadaEn", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "version", ignore = true)
    void actualizarEntidad(
            GuardarPreferenciaUbicacionDTO dto,
            @MappingTarget PreferenciaUbicacion entidad
    );
}
