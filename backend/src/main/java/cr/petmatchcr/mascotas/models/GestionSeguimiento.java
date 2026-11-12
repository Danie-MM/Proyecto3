package cr.petmatchcr.mascotas.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
public class GestionSeguimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID gestionSeguimientoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adopcion_id")
    private Adopcion adopcionId;

    @OneToOne
    @JoinColumn(name = "responsable_usuario_id", referencedColumnName = "usuario_id", nullable = false)
    private Usuario responsableUsuarioId;

    @Column(nullable = false)
    private LocalDateTime fechaGestion;

    @Column(nullable = false)
    private String medio;

    @Column(nullable = false)
    private String resutlado;

    @Column(nullable = false)
    private String observaciones;

    @Column
    private int consentimientoVersion;
}
