package cr.petmatchcr.campanias;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "campanias")
public class Campania {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "La organización es obligatoria")
    @Column(name = "organizacion_id", nullable = false)
    private UUID organizacionId;

    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nombre;

    @NotBlank(message = "La descripción no puede estar vacía")
    @Size(min = 1, max = 2000, message = "La descripción debe tener entre 1 y 2000 caracteres")
    @Column(nullable = false, length = 2000)
    private String descripcion;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false, length = 50)
    private String estado;

    @Column(name = "creado_por_id", nullable = false)
    private UUID creadoPorId;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Version
    @Column(nullable = false)
    private Long version;

    // --- Lógica automática antes de guardar (Requisito del profesor) ---
    @PrePersist
    protected void onCreate() {
        this.estado = "ACTIVA";
        this.creadoEn = OffsetDateTime.now(ZoneOffset.UTC);
    }

    // --- Getters y Setters ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrganizacionId() { return organizacionId; }
    public void setOrganizacionId(UUID organizacionId) { this.organizacionId = organizacionId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public UUID getCreadoPorId() { return creadoPorId; }
    public void setCreadoPorId(UUID creadoPorId) { this.creadoPorId = creadoPorId; }

    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(OffsetDateTime creadoEn) { this.creadoEn = creadoEn; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}