package cr.petmatchcr.mascotas.publicacion;

import cr.petmatchcr.mascotas.demo.ActoresDemoB01;
import cr.petmatchcr.mascotas.service.ContextoMascotas;
import cr.petmatchcr.mascotas.service.MascotaService;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrganizacionesDemoPublicacionTest {
    @Test
    void elRepresentantePuedePublicarANubeConElPadronRealDeLaDemo() {
        var actores = new ActoresDemoB01();
        var representante = actores.listar().stream().filter(a -> a.nombre().equals("Representante del Refugio A"))
                .findFirst().orElseThrow();
        var refugio = representante.organizaciones().getFirst();
        var nube = UUID.fromString("10000000-0000-4000-8000-000000000003");
        var contexto = mock(ContextoMascotas.class);
        var actor = new ContextoMascotas.Actor(representante.id(), actores.organizacionesAdministrables(representante.id()));
        when(contexto.actual()).thenReturn(actor);
        var repo = mock(PublicacionRepository.class);
        when(repo.bloquearAutorizada(nube,actor)).thenReturn(
                new PublicacionRepository.Estado(0,true,"NO_DISPONIBLE",refugio.id()));
        var servicio = new PublicacionService(repo,contexto,mock(MascotaService.class),actores);

        servicio.disponibilidad(nube,0,"DISPONIBLE");
        verify(repo).disponibilidad(nube,"DISPONIBLE");
        when(repo.publicable(nube)).thenReturn(Optional.of(new PublicacionRepository.Perfil(nube,"Nube","GATO",
                12,"PEQUENO","MEDIA",refugio.id(),false,List.of())));
        assertThat(servicio.consultarPerfilPublico(nube).organizacion().nombre()).isEqualTo(refugio.nombre());
        assertThat(actores.nombreOrganizacionActiva(UUID.randomUUID())).isEmpty();
    }
}
