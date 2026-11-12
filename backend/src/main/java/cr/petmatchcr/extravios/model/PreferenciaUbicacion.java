package cr.petmatchcr.extravios.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "preferencia_ubicacion")
public class PreferenciaUbicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column (name = "usuario_id", nullable = false, unique = true)
    private UUID usuarioId;

    //Entre -90 y 90. Nula solo si longitud también es nula
    @Column(precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(length = 200)
    private String referencia;

    @Column (name = "acepta_avisos", nullable = false)
    private boolean aceptaAvisos;

    @Column(name = "consentimiento_en")
    private Instant consentimientoEn;

    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    @Column(name="creado_en", nullable = false)
    private Instant creadoEn;

    @Version
    @Column(nullable = false)
    private Integer version;

    /** True cuando hay un punto completo; omitir la ubicación es válido. */
    public boolean tieneUbicacion() {
        return latitud != null && longitud != null;
    }

    /** Sin punto o sin aceptación, el usuario queda fuera de los avisos por cercanía. */
    public boolean recibeAvisosPorProximidad() {
        return aceptaAvisos && tieneUbicacion();
    }

}
