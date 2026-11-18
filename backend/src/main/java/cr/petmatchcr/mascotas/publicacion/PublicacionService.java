package cr.petmatchcr.mascotas.publicacion;

import cr.petmatchcr.mascotas.api.AmbitosMascotas;
import cr.petmatchcr.mascotas.api.MascotasParaAdopciones;
import cr.petmatchcr.mascotas.dto.MascotaPrivadaRespuesta;
import cr.petmatchcr.mascotas.dto.MascotaPublicaRespuesta;
import cr.petmatchcr.mascotas.service.ContextoMascotas;
import cr.petmatchcr.mascotas.service.ErrorMascota;
import cr.petmatchcr.mascotas.service.MascotaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PublicacionService implements MascotasParaAdopciones {
    private final PublicacionRepository repo;
    private final ContextoMascotas contexto;
    private final MascotaService privadas;
    private final AmbitosMascotas ambitos;

    PublicacionService(PublicacionRepository repo, ContextoMascotas contexto, MascotaService privadas,
                       AmbitosMascotas ambitos) {
        this.repo = repo;
        this.contexto = contexto;
        this.privadas = privadas;
        this.ambitos = ambitos;
    }

    public record FotoGuardada(UUID id, UUID mascotaId, String urlPrivada, int tamanoBytes,
                              String mimeVerificado, int versionMascota) {}
    public record FotoRetirada(FotoGuardada foto, int versionMascota) {}

    @Transactional
    public MascotaPrivadaRespuesta disponibilidad(UUID mascota, int version, String valor) {
        var actual = repo.bloquearAutorizada(mascota, contexto.actual());
        comprobarVersion(actual, version);
        if ("ADOPTADA".equals(actual.disponibilidad()))
            throw conflicto("Una adopción formalizada no puede cambiarse aquí.");
        if ("DISPONIBLE".equals(valor)) {
            if (actual.organizacion() == null || ambitos.nombreOrganizacionActiva(actual.organizacion()).isEmpty())
                throw conflicto("Se requiere una organización responsable activa para publicar.");
        } else if (!"NO_DISPONIBLE".equals(valor)) {
            throw new ErrorMascota(400, "VALIDACION", "Elegí una disponibilidad válida.");
        }
        if (!valor.equals(actual.disponibilidad())) repo.disponibilidad(mascota, valor);
        return privadas.consultar(mascota);
    }

    @Override
    public MascotaPublicaRespuesta consultarPerfilPublico(UUID mascota) {
        return perfilPublico(mascota).orElseThrow(PublicacionRepository::ausente);
    }

    @Override
    public boolean esPublicable(UUID mascota) { return perfilPublico(mascota).isPresent(); }

    private Optional<MascotaPublicaRespuesta> perfilPublico(UUID mascota) {
        return repo.publicable(mascota).flatMap(p -> ambitos.nombreOrganizacionActiva(p.organizacion()).map(nombre ->
                new MascotaPublicaRespuesta(p.id(), p.nombre(), p.especie(), p.edad(), p.tamano(), p.energia(),
                        p.etiquetas(), p.foto() ? url(mascota) + "/publica" : null,
                        new MascotaPublicaRespuesta.Organizacion(p.organizacion(), nombre), "DISPONIBLE")));
    }

    @Transactional
    public FotoGuardada cargar(UUID mascota, UUID clave, int version, MultipartFile archivo) {
        var actor = contexto.actual();
        var actual = repo.bloquearAutorizada(mascota, actor);
        var foto = FotografiaVerificada.leer(archivo);
        var nuevo = new PublicacionRepository.Recibo(mascota, version, sha256(foto.contenido()), UUID.randomUUID(),
                foto.mime(), foto.contenido().length);
        var recibo = aplicar(actor.usuarioId(), clave, actual, nuevo, foto);
        return new FotoGuardada(recibo.archivo(), mascota, url(mascota), recibo.tamano(), recibo.mime(),
                recibo.versionEsperada() + 1);
    }

    @Transactional
    public FotoRetirada retirar(UUID mascota, UUID clave, int version) {
        var actor = contexto.actual();
        var actual = repo.bloquearAutorizada(mascota, actor);
        var nuevo = new PublicacionRepository.Recibo(mascota, version, new byte[0], null, null, null);
        var recibo = aplicar(actor.usuarioId(), clave, actual, nuevo, null);
        return new FotoRetirada(null, recibo.versionEsperada() + 1);
    }

    private PublicacionRepository.Recibo aplicar(UUID actor, UUID clave, PublicacionRepository.Estado actual,
            PublicacionRepository.Recibo nuevo, FotografiaVerificada foto) {
        var anterior = repo.reservarClave(actor, clave, nuevo);
        if (anterior.isPresent()) {
            var recibo = anterior.get();
            if (!recibo.mascota().equals(nuevo.mascota()) || recibo.versionEsperada() != nuevo.versionEsperada()
                    || !Arrays.equals(recibo.huella(), nuevo.huella()))
                throw new ErrorMascota(409, "CLAVE_REUTILIZADA", "Esa clave ya se usó con otros datos.");
            return recibo;
        }
        comprobarVersion(actual, nuevo.versionEsperada());
        repo.reemplazar(nuevo, actor, foto);
        return nuevo;
    }

    public PublicacionRepository.Foto fotoPrivada(UUID mascota) {
        return repo.fotoPrivada(mascota, contexto.actual());
    }

    public PublicacionRepository.Foto fotoPublica(UUID mascota) {
        var foto = repo.fotoPublica(mascota);
        if (ambitos.nombreOrganizacionActiva(foto.organizacion()).isEmpty()) throw PublicacionRepository.ausente();
        var original = foto.archivo();
        return new PublicacionRepository.Foto(
                ImagenPublica.sinMetadatosPrivados(original.bytes(), original.mime()), original.mime());
    }

    private static void comprobarVersion(PublicacionRepository.Estado actual, int version) {
        if (version != actual.version())
            throw new ErrorMascota(409, "VERSION_DESACTUALIZADA", "La mascota cambió. Recargá el perfil.");
        if (!actual.activa()) throw conflicto("La mascota está desactivada.");
    }

    private static ErrorMascota conflicto(String mensaje) { return new ErrorMascota(409, "CONFLICTO", mensaje); }
    static String url(UUID mascota) { return "/api/v1/mascotas/" + mascota + "/foto"; }
    private static byte[] sha256(byte[] bytes) {
        try { return MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
