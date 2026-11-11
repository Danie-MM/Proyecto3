package cr.petmatchcr.mascotas.service;

import cr.petmatchcr.mascotas.api.MascotasParaExtravios;
import cr.petmatchcr.mascotas.api.ResumenMascotaExtravio;
import cr.petmatchcr.mascotas.dto.*;
import cr.petmatchcr.mascotas.model.Mascota;
import cr.petmatchcr.mascotas.repository.MascotaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Coordina cada caso de uso. El controlador recibe HTTP, este servicio obtiene al actor
 * y el repositorio ejecuta SQL. La transacción mantiene perfil y etiquetas consistentes.
 */
@Service
@Transactional(readOnly = true)
public class MascotaService implements MascotasParaExtravios {
    private final MascotaRepository mascotas;
    private final ContextoMascotas contexto;

    public MascotaService(MascotaRepository mascotas, ContextoMascotas contexto) {
        this.mascotas = mascotas;
        this.contexto = contexto;
    }

    public PaginaMascotasRespuesta listar(int pagina, int limite, UUID organizacionId, Boolean activas) {
        if (pagina < 0 || limite < 1 || limite > 100 || (long) pagina * limite > Integer.MAX_VALUE) {
            throw new ErrorMascota(400, "VALIDACION", "Usá página desde 0 y límite entre 1 y 100.");
        }
        var actor = contexto.actual();
        comprobarOrganizacion(organizacionId, actor);
        var lista = mascotas.listar(actor.usuarioId(), actor.organizaciones(), organizacionId, activas, pagina, limite);
        return new PaginaMascotasRespuesta(lista.stream().map(MascotaPrivadaRespuesta::desde).toList(),
                pagina, limite, mascotas.contar(actor.usuarioId(), actor.organizaciones(), organizacionId, activas));
    }

    public MascotaPrivadaRespuesta consultar(UUID id) {
        return MascotaPrivadaRespuesta.desde(buscarAutorizada(id));
    }

    @Transactional
    public MascotaPrivadaRespuesta crear(UUID clave, CrearMascotaSolicitud solicitud) {
        var actor = contexto.actual();
        comprobarOrganizacion(solicitud.organizacionId(), actor);
        return MascotaPrivadaRespuesta.desde(mascotas.crear(actor.usuarioId(), actor.organizaciones(), clave, solicitud));
    }

    @Transactional
    public MascotaPrivadaRespuesta editar(UUID id, EditarMascotaSolicitud solicitud) {
        var actor = contexto.actual();
        return MascotaPrivadaRespuesta.desde(mascotas.editar(id, actor.usuarioId(), actor.organizaciones(), solicitud));
    }

    @Transactional
    public MascotaPrivadaRespuesta desactivar(UUID id, UUID clave, DesactivarMascotaSolicitud solicitud) {
        var actor = contexto.actual();
        return MascotaPrivadaRespuesta.desde(mascotas.desactivar(id, actor.usuarioId(), actor.organizaciones(), clave, solicitud));
    }

    private Mascota buscarAutorizada(UUID id) {
        var actor = contexto.actual();
        return mascotas.consultar(id, actor.usuarioId(), actor.organizaciones())
                .orElseThrow(() -> new ErrorMascota(404, "NO_ENCONTRADO", "No encontramos esa mascota."));
    }

    private void comprobarOrganizacion(UUID organizacionId, ContextoMascotas.Actor actor) {
        if (organizacionId != null && !actor.organizaciones().contains(organizacionId)) {
            throw new ErrorMascota(403, "NO_AUTORIZADO", "No podés administrar ese ámbito.");
        }
    }

    @Override
    public void comprobarResponsable(UUID mascotaId) { buscarDelResponsable(mascotaId); }

    @Override
    public ResumenMascotaExtravio consultarResumenParaExtravio(UUID mascotaId) {
        var mascota = buscarDelResponsable(mascotaId);
        if (!mascota.isActiva()) throw new ErrorMascota(409, "CONFLICTO", "La mascota está desactivada.");
        return new ResumenMascotaExtravio(mascota.getId(), mascota.getNombre(), mascota.getEspecie().getCodigo());
    }

    /** C requiere al responsable vigente; representar la misma organización no basta. */
    private Mascota buscarDelResponsable(UUID id) {
        var actor = contexto.actual();
        return mascotas.consultar(id, actor.usuarioId(), actor.organizaciones())
                .filter(mascota -> actor.usuarioId().equals(mascota.getResponsableUsuarioId()))
                .orElseThrow(() -> new ErrorMascota(404, "NO_ENCONTRADO", "No encontramos esa mascota."));
    }
}
