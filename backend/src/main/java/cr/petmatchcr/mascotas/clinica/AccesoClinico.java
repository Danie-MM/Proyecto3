package cr.petmatchcr.mascotas.clinica;

import java.util.UUID;

/** Integración con A-03. La implementación consulta el rol vigente y participa en la transacción de B. */
public interface AccesoClinico {
    boolean esVeterinarioHabilitado(UUID usuarioId);

    enum Accion { CONCEDER_PERMISO_CLINICO, REVOCAR_PERMISO_CLINICO }

    /** Si falla la escritura en la auditoría común de A, propagar la excepción para revertir el permiso. */
    void auditarPermiso(Accion accion, UUID actorId, UUID permisoId, UUID mascotaId);
}
