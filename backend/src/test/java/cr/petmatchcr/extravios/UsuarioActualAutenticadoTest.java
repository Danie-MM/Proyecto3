package cr.petmatchcr.extravios;

import cr.petmatchcr.extravios.service.ErrorUbicacion;
import cr.petmatchcr.extravios.service.UsuarioActualAutenticado;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** C falla cerrado si A ya no reconoce una cuenta vigente. */
class UsuarioActualAutenticadoTest {
    @Test
    void utilizaElIdentificadorRealCompartido() {
        UUID actor = UUID.randomUUID();
        var usuario = new UsuarioActualAutenticado(() -> Optional.of(actor));
        assertThat(usuario.obtenerUsuarioId()).isEqualTo(actor);
    }

    @Test
    void unaIdentidadAusenteNoSeSustituyePorUnUsuarioDePrueba() {
        var usuario = new UsuarioActualAutenticado(Optional::empty);
        assertThatThrownBy(usuario::obtenerUsuarioId).isInstanceOfSatisfying(ErrorUbicacion.class,
                error -> assertThat(error.getEstado()).isEqualTo(401));
    }
}
