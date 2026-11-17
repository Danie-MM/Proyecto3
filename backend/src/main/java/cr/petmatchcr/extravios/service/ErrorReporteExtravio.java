
package cr.petmatchcr.extravios.service;

/**
 * Representa un rechazo controlado de las operaciones
 * relacionadas con reportes de extravío.
 */
public class ErrorReporteExtravio extends RuntimeException {

    private final int estado;
    private final String codigo;

    public ErrorReporteExtravio(
            int estado,
            String codigo,
            String mensaje
    ) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public int getEstado() {
        return estado;
    }

    public String getCodigo() {
        return codigo;
    }
}

