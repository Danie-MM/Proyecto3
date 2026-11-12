package cr.petmatchcr.mascotas.model;

import java.util.UUID;

/**
 * Representa una especie del catálogo.
 * Sus valores se obtendrán de la base de datos.
 */

public class Especie {

    // Identifica de forma única el registro del catálogo.
    private UUID id;

    // Código estable que utilizan las operaciones del sistema.
    private String codigo;

    // Nombre que se muestra a las personas en el formulario.
    private String nombre;

    // Indica si se permite seleccionar esta especie en nuevas altas.
    private boolean activa;

    /**
     * Construye una especie con los datos del catálogo.
     * Crear este objeto no lo guarda en la base de datos.
     *
     * @param id identificador de la especie
     * @param codigo código estable utilizado por el sistema
     * @param nombre nombre visible en el formulario
     * @param activa indica si puede seleccionarse en nuevas altas
     */

    public Especie(UUID id, String codigo, String nombre, boolean activa) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.activa = activa;
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActiva() {
        return activa;
    }

}
