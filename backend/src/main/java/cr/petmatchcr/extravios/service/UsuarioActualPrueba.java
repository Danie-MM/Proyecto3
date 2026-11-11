package cr.petmatchcr.extravios.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("dev")
public class UsuarioActualPrueba implements UsuarioActual {

    @Override
    public UUID obtenerUsuarioId() {
        return UUID.fromString(
                "11111111-1111-1111-1111-111111111111"
        );
    }
}
