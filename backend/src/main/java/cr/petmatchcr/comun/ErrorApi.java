package cr.petmatchcr.comun;

import java.util.List;
import java.util.UUID;

public record ErrorApi(String codigo, String mensaje, List<Campo> erroresCampos, UUID trazaId) {
    public record Campo(String campo, String mensaje) {}

    public static ErrorApi crear(String codigo, String mensaje, Campo... campos) {
        return new ErrorApi(codigo, mensaje, List.of(campos), UUID.randomUUID());
    }
}
