package cr.petmatchcr.mascotas.clinica;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cr.petmatchcr.mascotas.api.AmbitosMascotas;
import cr.petmatchcr.mascotas.service.ContextoMascotas;
import cr.petmatchcr.mascotas.service.ErrorMascota;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifica el límite con A aun en instalaciones sin un servicio de roles/auditoría integrado. */
class B03ServicioClinicoTest {
    private final ContextoMascotas contexto = mock(ContextoMascotas.class);
    private final AmbitosMascotas ambitos = mock(AmbitosMascotas.class);
    private final RepositorioClinico repo = mock(RepositorioClinico.class);
    private final ContextoMascotas.Actor actor = new ContextoMascotas.Actor(UUID.randomUUID(), Set.of());
    private final UUID mascota = UUID.randomUUID();
    private final UUID clave = UUID.randomUUID();
    private final ServicioClinico sinA03 = new ServicioClinico(contexto, ambitos, repo, Optional.empty());

    @BeforeEach
    void actorActual() { when(contexto.actual()).thenReturn(actor); }

    @Test
    void sinAuditoriaNiVerificadorNoHayConcesionesParciales() {
        when(repo.mascota(mascota, actor, true)).thenReturn(Optional.of(new RepositorioClinico.MascotaClinica(true, true, null)));
        var entrada = new DatosClinicos.Concesion(UUID.randomUUID(), "VETERINARIO_AUTORIZADO", "LECTURA");
        assertThatThrownBy(() -> sinA03.conceder(mascota, clave, entrada)).isInstanceOfSatisfying(ErrorMascota.class,
                e -> assertThat(e.getEstado()).isEqualTo(503));
        verify(repo, never()).conceder(any(), any(), any(), any());
    }

    @Test
    void unPermisoPersistidoNoSustituyeLaHabilitacionVigenteDeA() {
        when(repo.mascota(mascota, actor, false)).thenReturn(Optional.of(new RepositorioClinico.MascotaClinica(false, true, null)));
        when(repo.alcance(mascota, actor.usuarioId())).thenReturn(Optional.of("LECTURA_ESCRITURA"));
        assertThatThrownBy(() -> sinA03.listarEventos(mascota, null,null,null,0,20))
                .isInstanceOfSatisfying(ErrorMascota.class, e -> assertThat(e.getEstado()).isEqualTo(404));
        verify(repo, never()).eventos(any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void elTitularPuedeRegistrarSinA03YElServidorFijaElOrigen() {
        when(repo.mascota(mascota, actor, true)).thenReturn(Optional.of(new RepositorioClinico.MascotaClinica(true, true, null)));
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Costa_Rica"));
        String nombre = "🐾".repeat(2000);
        var entrada = new DatosClinicos.NuevaVacuna("VACUNA", hoy, "  " + nombre + " ", null);
        var normalizada = new DatosClinicos.NuevaVacuna("VACUNA", hoy, nombre, null);
        UUID eventoId = UUID.randomUUID();
        when(repo.registrar(mascota,actor.usuarioId(),"DECLARADO_RESPONSABLE",normalizada,clave)).thenReturn(eventoId);
        var respuesta = mock(DatosClinicos.Evento.class);
        when(repo.evento(eventoId)).thenReturn(Optional.of(respuesta));
        assertThat(sinA03.registrar(mascota,clave,entrada)).isSameAs(respuesta);
        assertThatThrownBy(() -> sinA03.registrar(mascota,clave,new DatosClinicos.NuevaVacuna("VACUNA",hoy.plusDays(1),"X",null)))
                .isInstanceOfSatisfying(ErrorMascota.class, e -> assertThat(e.getEstado()).isEqualTo(400));
    }
}
