package cr.petmatchcr.extravios.service;

/** Rechazo controlado del contrato C-01, separado de los errores de infraestructura. */
public class ErrorUbicacion extends RuntimeException {
    private final int estado;
    private final String codigo;

    public ErrorUbicacion(int estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public int getEstado() { return estado; }

    public String getCodigo() { return codigo; }
}
