package cr.petmatchcr.extravios.service;

import cr.petmatchcr.acceso.api.IdentidadActual;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Conecta C con la cuenta vigente de A; nunca toma el usuario del formulario. */
@Component
@RequiredArgsConstructor
public class UsuarioActualAutenticado implements UsuarioActual {
    private final IdentidadActual identidad;

    @Override
    public UUID obtenerUsuarioId() {
        return identidad.usuarioActual().orElseThrow(() -> new ErrorUbicacion(
                401, "NO_AUTENTICADO", "Iniciá sesión para consultar o modificar tu ubicación."));
    }
}
