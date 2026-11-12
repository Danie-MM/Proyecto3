package cr.petmatchcr.mascotas.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Adopcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID adopcionId;

    @OneToOne
    @JoinColumn(name = "solicitud_adopcion_id", referencedColumnName = "solicitud_adopcion_id", nullable = false)
    private SolicitudAdopcion solicitudId;

    @OneToOne
    @JoinColumn(name = "mascota_id", referencedColumnName = "mascota_id", nullable = false)
    private Mascota mascotaId;

    @Column(nullable = false)
    private UUID adoptanteUsuarioId;

    @Column(nullable = false)
    private UUID organizacionId;

    @Column(nullable = false)
    private LocalDateTime fechaEfectiva;

    @Column(nullable = false)
    private UUID formalizadaPorUsuarioId;

    @Column(nullable = false)
    private boolean vigente;
}
