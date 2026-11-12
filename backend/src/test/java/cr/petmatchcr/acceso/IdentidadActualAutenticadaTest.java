package cr.petmatchcr.acceso;

import cr.petmatchcr.acceso.entities.Usuario;
import cr.petmatchcr.acceso.repositories.UsuarioRepository;
import cr.petmatchcr.acceso.services.IdentidadActualAutenticada;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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

    private void autenticar(String nombre) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(nombre, null, List.of()));
    }
}
