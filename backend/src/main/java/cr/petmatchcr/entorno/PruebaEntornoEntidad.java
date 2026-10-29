package cr.petmatchcr.entorno;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Permite a Hibernate validar la tabla; las operaciones usan las rutinas SQL. */
@Entity
@Table(name = "prueba_entorno")
public class PruebaEntornoEntidad {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String texto;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected PruebaEntornoEntidad() {
        // JPA necesita este constructor; no usamos save() para saltarnos las rutinas SQL.
    }
}
