package cr.petmatchcr.mascotas.service;

/** Error de negocio controlado; nunca contiene SQL ni detalles de infraestructura. */
public class ErrorMascota extends RuntimeException {
    private final int estado;
    private final String codigo;

    public ErrorMascota(int estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public int getEstado() { return estado; }
    public String getCodigo() { return codigo; }
}
