package cr.petmatchcr.mascotas.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


public class TestCompatibilidad {

    /*@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long testCompatibilidadId;

    @ManyToOne
    @JoinColumn(name = "adoptante_usuario_id", nullable = false)
    private Usuario adoptanteUsuarioId;

    @OneToMany(mappedBy = "test_compatibildad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VersionCompatibilidad> versionCompatibilidadId;

    @Column(nullable = false)
    private String respuestas;

    @Column(nullable = false)
    private LocalDateTime completadoEn;*/
}
