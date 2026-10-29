package cr.petmatchcr.entorno;

/** Indica que un identificador válido no tiene registro en la base de datos. */
public class PruebaEntornoNoEncontradaException extends RuntimeException {

    /** Usa un mensaje que la pantalla puede mostrar al estudiante. */
    public PruebaEntornoNoEncontradaException() {
        super("No encontramos ese registro");
    }
}
