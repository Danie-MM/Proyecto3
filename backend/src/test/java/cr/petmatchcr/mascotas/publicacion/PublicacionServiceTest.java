package cr.petmatchcr.mascotas.publicacion;

import cr.petmatchcr.mascotas.api.AmbitosMascotas;
import cr.petmatchcr.mascotas.dto.MascotaPublicaRespuesta;
import cr.petmatchcr.mascotas.service.ContextoMascotas;
import cr.petmatchcr.mascotas.service.ErrorMascota;
import cr.petmatchcr.mascotas.service.MascotaService;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PublicacionServiceTest {
    private final PublicacionRepository repo = mock(PublicacionRepository.class);
    private final ContextoMascotas contexto = mock(ContextoMascotas.class);
    private final AmbitosMascotas ambitos = mock(AmbitosMascotas.class);
    private final MascotaService privadas = mock(MascotaService.class);
    private final PublicacionService servicio = new PublicacionService(repo, contexto, privadas, ambitos);
    private final UUID mascota = UUID.randomUUID();
    private final UUID org = UUID.randomUUID();
    private final ContextoMascotas.Actor actor = new ContextoMascotas.Actor(UUID.randomUUID(), Set.of(org));

    @Test
    void laMismaUrlDejaDeEntregarPerfilYFotoCuandoLaOrganizacionPierdeVigencia() throws Exception {
        var etiqueta = new MascotaPublicaRespuesta.Etiqueta(UUID.randomUUID(), "Sociable");
        var perfil = new PublicacionRepository.Perfil(mascota, "Luna", "PERRO", 4, "MEDIANO",
                "BAJA", org, true, List.of(etiqueta));
        var archivo = new PublicacionRepository.Foto(FotografiaVerificadaTest.imagen("png"), "image/png");
        when(repo.publicable(mascota)).thenReturn(Optional.of(perfil));
        when(repo.fotoPublica(mascota)).thenReturn(new PublicacionRepository.FotoPublica(org, archivo));
        when(ambitos.nombreOrganizacionActiva(org)).thenReturn(Optional.of("Refugio activo"));

        var publica = servicio.consultarPerfilPublico(mascota);
        assertThat(publica.fotoUrl()).isEqualTo("/api/v1/mascotas/" + mascota + "/foto/publica");
        assertThat(publica.comportamientos()).containsExactly(etiqueta);
        assertThat(publica.organizacion().nombre()).isEqualTo("Refugio activo");
        assertThat(servicio.fotoPublica(mascota).bytes()).isEqualTo(archivo.bytes());

        when(ambitos.nombreOrganizacionActiva(org)).thenReturn(Optional.empty());
        assertThat(servicio.esPublicable(mascota)).isFalse();
        assertThatExceptionOfType(ErrorMascota.class).isThrownBy(() -> servicio.fotoPublica(mascota))
                .extracting(ErrorMascota::getEstado).isEqualTo(404);
        verifyNoInteractions(privadas);
    }

    @Test
    void publicarExigeVersionVigenteYOrganizacionActiva() {
        when(contexto.actual()).thenReturn(actor);
        when(repo.bloquearAutorizada(mascota,actor)).thenReturn(
                new PublicacionRepository.Estado(3,true,"NO_DISPONIBLE",org));
        assertThatExceptionOfType(ErrorMascota.class).isThrownBy(() -> servicio.disponibilidad(mascota,2,"DISPONIBLE"))
                .extracting(ErrorMascota::getEstado).isEqualTo(409);
        assertThatExceptionOfType(ErrorMascota.class).isThrownBy(() -> servicio.disponibilidad(mascota,3,"DISPONIBLE"))
                .extracting(ErrorMascota::getEstado).isEqualTo(409);
        verify(repo,never()).disponibilidad(any(),any());

        when(ambitos.nombreOrganizacionActiva(org)).thenReturn(Optional.of("Refugio"));
        servicio.disponibilidad(mascota,3,"DISPONIBLE");
        verify(repo).disponibilidad(mascota,"DISPONIBLE");
        verify(privadas).consultar(mascota);
    }

    @Test
    void unReintentoDeRetiroAutorizadoDevuelveElReciboSinRepetirLaEscritura() {
        var clave = UUID.randomUUID();
        when(contexto.actual()).thenReturn(actor);
        when(repo.bloquearAutorizada(mascota,actor)).thenReturn(
                new PublicacionRepository.Estado(9,true,"NO_DISPONIBLE",org));
        when(repo.reservarClave(eq(actor.usuarioId()),eq(clave),any())).thenReturn(Optional.of(
                new PublicacionRepository.Recibo(mascota,2,new byte[0],null,null,null)));
        assertThat(servicio.retirar(mascota,clave,2).versionMascota()).isEqualTo(3);
        assertThatExceptionOfType(ErrorMascota.class).isThrownBy(() -> servicio.retirar(mascota,clave,4))
                .extracting(ErrorMascota::getEstado).isEqualTo(409);
        verify(repo,never()).reemplazar(any(),any(),any());
    }
}
