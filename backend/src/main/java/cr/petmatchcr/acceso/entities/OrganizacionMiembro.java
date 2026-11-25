package cr.petmatchcr.acceso.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Versión: 1.0
 * Autora: Daniela Morales
 * Descripción: Relaciona usuarios con organizaciones y sus permisos.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organizacion_miembros")
public class OrganizacionMiembro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacion_id", nullable = false)
    private Organizacion organizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "puede_administrar", nullable = false)
    private boolean puedeAdministrar = false;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private OffsetDateTime creadoEn;
}
