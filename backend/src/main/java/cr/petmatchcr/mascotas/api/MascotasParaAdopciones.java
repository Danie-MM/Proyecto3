package cr.petmatchcr.mascotas.api;

import cr.petmatchcr.mascotas.dto.MascotaPublicaRespuesta;
import cr.petmatchcr.mascotas.dto.FiltrosCatalogoPublico;
import cr.petmatchcr.mascotas.dto.PaginaMascotasPublicas;
import java.util.UUID;

/** Contrato de lectura que D puede consumir sin acceder a un perfil privado. */
public interface MascotasParaAdopciones {
    MascotaPublicaRespuesta consultarPerfilPublico(UUID mascotaId);
    PaginaMascotasPublicas consultarCatalogo(FiltrosCatalogoPublico filtros, int pagina, int limite);
    boolean esPublicable(UUID mascotaId);
}
