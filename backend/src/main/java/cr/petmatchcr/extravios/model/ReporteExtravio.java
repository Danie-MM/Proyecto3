
package cr.petmatchcr.extravios.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Representa un reporte de extravío publicado por el
 * responsable autorizado de una mascota.
 */
@Entity
@Getter
@Setter
@Table(name = "reporte_extravio")
public class ReporteExtravio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @Column(name = "mascota_id", nullable = false)
    private UUID mascotaId;

    @Column(name = "creado_por_usuario_id", nullable = false)
    private UUID creadoPorUsuarioId;

    @Column(name = "fecha_extravio", nullable = false)
    private Instant fechaExtravio;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(name = "latitud_origen_aviso", nullable = false,
            precision = 9, scale = 6)
    private BigDecimal latitudOrigenAviso;

    @Column(name = "longitud_origen_aviso", nullable = false,
            precision = 9, scale = 6)
    private BigDecimal longitudOrigenAviso;

    @Column(name = "radio_metros", nullable = false)
    private Integer radioMetros;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contacto", nullable = false, length = 40)
    private MedioContacto tipoContacto;

    @Column(nullable = false, length = 254)
    private String contacto;

    @Column(name = "contacto_autorizado_en", nullable = false)
    private Instant contactoAutorizadoEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EstadoReporteExtravio estado;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Column(name = "cerrado_en")
    private Instant cerradoEn;

    @Column(name = "cerrado_por_usuario_id")
    private UUID cerradoPorUsuarioId;

    @Column(name = "motivo_cierre", length = 500)
    private String motivoCierre;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Version
    @Column(nullable = false)
    private Integer version;
}
