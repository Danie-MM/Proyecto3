package cr.petmatchcr.mascotas.api;

import java.util.UUID;

/** C consume este contrato interno, sin consultar las tablas de B ni hacer HTTP al propio servidor. */
public interface MascotasParaExtravios {
    void comprobarResponsable(UUID mascotaId);
    ResumenMascotaExtravio consultarResumenParaExtravio(UUID mascotaId);
}
