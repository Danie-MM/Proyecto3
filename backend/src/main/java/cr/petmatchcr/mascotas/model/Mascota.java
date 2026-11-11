package cr.petmatchcr.mascotas.model;

import java.util.UUID;
import java.time.Instant;
import java.util.List;

/**
 * Representa los datos de una mascota dentro del programa.
 * El guardado en la base de datos se realiza en el repositorio
 */

public class Mascota {

    // Identificador que recibiremos de la base de datos.

    private UUID id;

    // Datos descriptivos del perfil.

    private String nombre;
    private int edadEstimadaMeses;

    // Referencia a la especie del catálogo a la que pertenece la mascota.

    private Especie especie;

    // El perfil completo incluye estado y responsables; estos datos los fija el servidor.
    private Instant creadoEn;
    private int version;
    private String tamano;
    private String energia;
    private UUID responsableUsuarioId;
    private UUID organizacionId;
    private String disponibilidad;
    private boolean activa;
    private List<Comportamiento> comportamientos = List.of();

    /**
     * Construye una mascota con sus datos iniciales.
     * Crear este objeto no lo guarda en la base de datos.
     *
     * @param id identificador de la mascota
     * @param nombre nombre de la mascota
     * @param especie especie del catálogo a la que pertenece
     * @param edadEstimadaMeses edad aproximada expresada en meses
     */

    public Mascota(UUID id, String nombre, int edadEstimadaMeses, Especie especie) {
        this.id = id;
        this.nombre = nombre;
        this.edadEstimadaMeses = edadEstimadaMeses;
        this.especie = especie;
    }

    /**
     * Reconstruye el perfil que ya está guardado. El repositorio usa este constructor
     * al leer PostgreSQL; una respuesta HTTP se prepara después en un DTO.
     */
    public Mascota(UUID id, Instant creadoEn, int version, String nombre, Especie especie,
                   int edadEstimadaMeses, String tamano, String energia,
                   UUID responsableUsuarioId, UUID organizacionId, String disponibilidad,
                   boolean activa, List<Comportamiento> comportamientos) {
        this(id, nombre, edadEstimadaMeses, especie);
        this.creadoEn = creadoEn;
        this.version = version;
        this.tamano = tamano;
        this.energia = energia;
        this.responsableUsuarioId = responsableUsuarioId;
        this.organizacionId = organizacionId;
        this.disponibilidad = disponibilidad;
        this.activa = activa;
        this.comportamientos = List.copyOf(comportamientos);
    }


    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getEdadEstimadaMeses() {
        return edadEstimadaMeses;
    }

    public Especie getEspecie() {
        return especie;
    }

    // Getters: permiten consultar el perfil sin cambiar su estado desde el formulario.
    public Instant getCreadoEn() { return creadoEn; }
    public int getVersion() { return version; }
    public String getTamano() { return tamano; }
    public String getEnergia() { return energia; }
    public UUID getResponsableUsuarioId() { return responsableUsuarioId; }
    public UUID getOrganizacionId() { return organizacionId; }
    public String getDisponibilidad() { return disponibilidad; }
    public boolean isActiva() { return activa; }
    public List<Comportamiento> getComportamientos() { return comportamientos; }
    public List<UUID> getComportamientoIds() {
        return comportamientos.stream().map(Comportamiento::id).toList();
    }
}
