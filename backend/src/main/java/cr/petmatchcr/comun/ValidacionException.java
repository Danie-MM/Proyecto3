package cr.petmatchcr.comun;

/** Comunica una regla de entrada incumplida sin mezclarla con errores de la BD. */
public class ValidacionException extends RuntimeException {

    private final ErrorCampo errorCampo;

    /** Describe el campo incorrecto y la corrección que necesita. */
    public ValidacionException(String campo, String mensaje) {
        super(mensaje);
        this.errorCampo = new ErrorCampo(campo, mensaje);
    }

    /** Entrega el detalle que se incluirá en erroresCampos. */
    public ErrorCampo getErrorCampo() {
        return errorCampo;
    }
}
