package cr.petmatchcr.mascotas.clinica;

import static cr.petmatchcr.mascotas.clinica.DatosClinicos.*;

import cr.petmatchcr.mascotas.api.AmbitosMascotas;
import cr.petmatchcr.mascotas.service.ContextoMascotas;
import cr.petmatchcr.mascotas.service.ErrorMascota;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioClinico {
    private static final ZoneId COSTA_RICA = ZoneId.of("America/Costa_Rica");
    private final ContextoMascotas contexto;
    private final AmbitosMascotas ambitos;
    private final RepositorioClinico repo;
    private final Optional<AccesoClinico> acceso;

    ServicioClinico(ContextoMascotas contexto, AmbitosMascotas ambitos, RepositorioClinico repo, Optional<AccesoClinico> acceso) {
        this.contexto = contexto;
        this.ambitos = ambitos;
        this.repo = repo;
        this.acceso = acceso;
    }

    @Transactional
    public Evento registrar(UUID mascota, UUID clave, NuevaVacuna entrada) {
        var actor = contexto.actual();
        String origen = autorizarExpediente(mascota, actor, true);
        if (entrada == null || !"VACUNA".equals(entrada.tipo())) {
            throw invalido("En esta iteración solo se registran vacunas realizadas.");
        }
        if (entrada.fecha() == null || entrada.fecha().isAfter(LocalDate.now(COSTA_RICA))) {
            throw invalido("Indicá la fecha de la vacuna, hasta el día de hoy en Costa Rica.");
        }
        String nombre = texto(entrada.nombre(), 2000, "nombre");
        String descripcion = entrada.descripcion() == null ? null : texto(entrada.descripcion(), 2000, "descripción");
        var vacuna = new NuevaVacuna("VACUNA", entrada.fecha(), nombre, descripcion);
        UUID id = repo.registrar(mascota, actor.usuarioId(), origen, vacuna, clave);
        return repo.evento(id).orElseThrow();
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Pagina<Evento> listarEventos(UUID mascota, LocalDate desde, LocalDate hasta, String tipo, int pagina, int limite) {
        autorizarExpediente(mascota, contexto.actual(), false);
        validarPagina(pagina, limite);
        if (desde != null && hasta != null && hasta.isBefore(desde)) throw invalido("El rango de fechas está invertido.");
        if (tipo != null && !"VACUNA".equals(tipo)) throw invalido("El tipo disponible es VACUNA.");
        return repo.eventos(mascota, desde, hasta, tipo, pagina, limite);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Evento detalle(UUID id) {
        var actor = contexto.actual();
        var evento = repo.evento(id).orElseThrow(ServicioClinico::noEncontrado);
        autorizarExpediente(evento.mascotaId(), actor, false);
        return evento;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Pagina<Permiso> listarPermisos(UUID mascota, boolean soloActivos, int pagina, int limite) {
        exigirResponsable(mascota, contexto.actual(), false);
        validarPagina(pagina, limite);
        return repo.permisos(mascota, soloActivos, pagina, limite);
    }

    @Transactional
    public Permiso conceder(UUID mascota, UUID clave, Concesion solicitud) {
        var actor = contexto.actual();
        var perfil = exigirResponsable(mascota, actor, true);
        if (!perfil.activa()) throw conflictoInactivo();
        if (solicitud == null || solicitud.beneficiarioUsuarioId() == null
                || !"VETERINARIO_AUTORIZADO".equals(solicitud.fundamento())
                || !("LECTURA".equals(solicitud.alcance()) || "LECTURA_ESCRITURA".equals(solicitud.alcance()))) {
            throw invalido("Revisá el beneficiario, el fundamento y el alcance clínico.");
        }
        UUID beneficiario = solicitud.beneficiarioUsuarioId();
        if (actor.usuarioId().equals(beneficiario) || (perfil.organizacionId() != null
                && ambitos.organizacionesAdministrables(beneficiario).contains(perfil.organizacionId()))) {
            throw invalido("El responsable ya tiene acceso al expediente.");
        }
        AccesoClinico integracion = acceso.orElseThrow(ServicioClinico::faltaIntegracion);
        if (!integracion.esVeterinarioHabilitado(solicitud.beneficiarioUsuarioId())) {
            throw invalido("La cuenta indicada no está habilitada para recibir este permiso.");
        }
        var cambio = repo.conceder(mascota, actor.usuarioId(), solicitud, clave);
        if (cambio.aplicado()) {
            integracion.auditarPermiso(AccesoClinico.Accion.CONCEDER_PERMISO_CLINICO,
                    actor.usuarioId(), cambio.id(), mascota);
        }
        return repo.permiso(cambio.id()).orElseThrow();
    }

    @Transactional
    public Permiso revocar(UUID id, UUID clave, Revocacion solicitud) {
        var actor = contexto.actual();
        var permiso = repo.permiso(id).orElseThrow(ServicioClinico::noEncontrado);
        exigirResponsable(permiso.mascotaId(), actor, true);
        if (solicitud == null || solicitud.versionEsperada() == null || solicitud.versionEsperada() < 0) {
            throw invalido("Indicá la versión vigente del permiso.");
        }
        String motivo = texto(solicitud.motivo(), 500, "motivo");
        AccesoClinico integracion = acceso.orElseThrow(ServicioClinico::faltaIntegracion);
        var cambio = repo.revocar(id, actor.usuarioId(), motivo, solicitud.versionEsperada(), clave);
        if (cambio.aplicado()) {
            integracion.auditarPermiso(AccesoClinico.Accion.REVOCAR_PERMISO_CLINICO,
                    actor.usuarioId(), id, permiso.mascotaId());
        }
        return repo.permiso(id).orElseThrow();
    }

    private String autorizarExpediente(UUID mascota, ContextoMascotas.Actor actor, boolean escritura) {
        var perfil = repo.mascota(mascota, actor, escritura).orElseThrow(ServicioClinico::noEncontrado);
        String origen = "DECLARADO_RESPONSABLE";
        if (!perfil.responsable()) {
            var alcance = repo.alcance(mascota, actor.usuarioId()).orElseThrow(ServicioClinico::noEncontrado);
            if (!acceso.map(a -> a.esVeterinarioHabilitado(actor.usuarioId())).orElse(false)) throw noEncontrado();
            if (escritura && !"LECTURA_ESCRITURA".equals(alcance)) {
                throw new ErrorMascota(403, "NO_AUTORIZADO", "Tu permiso permite únicamente consultar el expediente.");
            }
            origen = "REGISTRADO_VETERINARIO";
        }
        if (escritura && !perfil.activa()) throw conflictoInactivo();
        return origen;
    }

    private RepositorioClinico.MascotaClinica exigirResponsable(UUID mascota, ContextoMascotas.Actor actor, boolean escritura) {
        return repo.mascota(mascota, actor, escritura).filter(RepositorioClinico.MascotaClinica::responsable)
                .orElseThrow(ServicioClinico::noEncontrado);
    }

    private static String texto(String valor, int maximo, String campo) {
        String texto = valor == null ? "" : valor.strip();
        if (texto.isEmpty() || texto.codePointCount(0, texto.length()) > maximo) {
            throw invalido("El campo " + campo + " debe tener entre 1 y " + maximo + " caracteres.");
        }
        return texto;
    }

    private static void validarPagina(int pagina, int limite) {
        if (pagina < 0 || limite < 1 || limite > 100) throw invalido("Revisá la página y el límite de 1 a 100.");
    }

    private static ErrorMascota invalido(String mensaje) { return new ErrorMascota(400, "VALIDACION", mensaje); }
    private static ErrorMascota noEncontrado() {
        return new ErrorMascota(404, "NO_ENCONTRADO", "No encontramos el recurso solicitado.");
    }
    private static ErrorMascota conflictoInactivo() {
        return new ErrorMascota(409, "CONFLICTO", "La mascota está desactivada.");
    }
    private static ErrorMascota faltaIntegracion() {
        return new ErrorMascota(503, "INTEGRACION_CLINICA_PENDIENTE",
                "La verificación de profesionales y la auditoría de permisos no están disponibles.");
    }
}
