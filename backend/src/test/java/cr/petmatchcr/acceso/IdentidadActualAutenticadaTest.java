package cr.petmatchcr.acceso;

import cr.petmatchcr.acceso.entities.Usuario;
import cr.petmatchcr.acceso.repositories.UsuarioRepository;
import cr.petmatchcr.acceso.services.IdentidadActualAutenticada;
import cr.petmatchcr.mascotas.demo.ActoresDemoB01;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IdentidadActualAutenticadaTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final IdentidadActualAutenticada identidad = new IdentidadActualAutenticada(usuarios);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resuelveElUuidPersistidoYRevalidaLaCuentaEnCadaConsulta() {
        var usuario = new Usuario();
        usuario.setId(17L);
        usuario.setEmail("responsable@example.test");
        UUID idPublico = usuario.getIdPublico();
        autenticar(usuario.getEmail());
        when(usuarios.findByEmailAndActivoTrue(usuario.getEmail()))
                .thenReturn(Optional.of(usuario), Optional.empty());

        assertThat(identidad.usuarioActual()).contains(idPublico);
        assertThat(identidad.usuarioActual()).isEmpty();
        verify(usuarios, times(2)).findByEmailAndActivoTrue(usuario.getEmail());
    }

    @Test
    void niUnUuidNiUnIdNumericoFabricanUnaIdentidad() {
        for (String nombre : List.of(UUID.randomUUID().toString(), "17", "ausente@example.test")) {
            autenticar(nombre);
            when(usuarios.findByEmailAndActivoTrue(nombre)).thenReturn(Optional.empty());
            assertThat(identidad.usuarioActual()).isEmpty();
        }
    }

    @Test
    void ausenciaDeAutenticacionYAnonimosNoConsultanUsuarios() {
        assertThat(identidad.usuarioActual()).isEmpty();
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "clave", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertThat(identidad.usuarioActual()).isEmpty();
        verifyNoInteractions(usuarios);
    }

    @Test
    void enDemoElPrincipalDeASeResuelveContraLaCuentaActivaYNoComoActorFicticio() {
        var demo = new ActoresDemoB01(identidad);
        var cuenta = new Usuario();
        cuenta.setEmail("propietaria@example.test");
        when(usuarios.findByEmailAndActivoTrue(cuenta.getEmail())).thenReturn(Optional.of(cuenta), Optional.empty());
        autenticar(User.withUsername(cuenta.getEmail()).password("prueba").roles("ADOPTANTE").build());
        assertThat(demo.usuarioActual()).contains(cuenta.getIdPublico());
        assertThat(demo.usuarioActual()).isEmpty();

        String actor = "33333333-3333-4333-8333-333333333333";
        autenticar(User.withUsername(actor).password("prueba").roles("ADOPTANTE").build());
        assertThat(demo.usuarioActual()).isEmpty();
        autenticar(actor);
        assertThat(demo.usuarioActual()).contains(UUID.fromString(actor));
        autenticar(cuenta.getEmail());
        assertThat(demo.usuarioActual()).isEmpty();
        verify(usuarios, times(2)).findByEmailAndActivoTrue(cuenta.getEmail());
        verify(usuarios).findByEmailAndActivoTrue(actor);
        verifyNoMoreInteractions(usuarios);
    }

    private void autenticar(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    }
}
