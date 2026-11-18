package cr.petmatchcr.mascotas.api;

import cr.petmatchcr.mascotas.dto.MascotaPublicaRespuesta;
import java.util.UUID;

/** Contrato de lectura que D puede consumir sin acceder a un perfil privado. */
public interface MascotasParaAdopciones {
    MascotaPublicaRespuesta consultarPerfilPublico(UUID mascotaId);
    boolean esPublicable(UUID mascotaId);
}
