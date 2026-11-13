package cr.petmatchcr.mascotas.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;


public class VersionCompatibilidad {

    /*@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long versionCompatibilidadId;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private int schemaVersion;

    @Column(nullable = false)
    private String cuestionario;

    @Column(nullable = false)
    private String factores;

    @Column(nullable = false)
    private String metodoCodigo;

    @Column(nullable = false)
    private String metodoDocumentado;

    @Column(nullable = false)
    private String evidencias;

    @Column(nullable = false)
    private String casosValidados;

    @ManyToOne
    @JoinColumn(name = "creada_por_usuario_id")
    private Usuario creadaPorUsuarioId;

    @Column
    private LocalDateTime activadaEn;

    @ManyToOne
    @JoinColumn(name = "activada_por_usuario_id")
    private Usuario activadaPorUsuarioId;

    @Column
    private LocalDateTime retiradaEn;

    @Column
    private String motivoRetiro;*/


}
