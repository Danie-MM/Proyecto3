package cr.petmatchcr.mascotas.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;

import java.util.UUID;

public class EventoSolicitudAdopcion {

    /*@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID eventoSolicitudAdopcionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_adopcion_id")
    private SolicitudAdopcion solicitudId;

    @OneToOne
    @JoinColumn(name = "adopcion_id", referencedColumnName = "adopcion_id")
    private Adopcion adopcionId;

    @OneToOne
    @JoinColumn(name = "actor_usuario_id", referencedColumnName = "usuario_id")
    private Usuario actorUsuarioId;

    @Column(nullable = false)
    private String accion;

    @Column
    private String estadoAnterior;

    @Column(nullable = false)
    private String estadoNuevo;

    @Column
    private String motivo;

    @Column
    private int versionAnterior;

    @Column(nullable = false)
    private int versionNueva;

    @Column
    private String detalleCorreccion;
    */
}
