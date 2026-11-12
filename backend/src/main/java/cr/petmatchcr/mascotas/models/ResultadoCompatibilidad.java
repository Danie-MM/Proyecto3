package cr.petmatchcr.mascotas.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
public class ResultadoCompatibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID resultadoCompatibilidadId;

    @Column(nullable = false)
    private UUID testCompatibilidadId;

    @ManyToOne
    @JoinColumn(name = "adoptante_usuario_id")
    private Usuario adoptanteUsuarioId;

    @OneToMany(mappedBy = "resultado_compatibilidad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VersionCompatibilidad> versionCompatibilidadId;

    @Column
    private String perfilesEvaluados;

    @Column
    private String recomendaciones;

    @Column
    private String limitaciones;
}
