package cr.petmatchcr.acceso.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Versión: 1.0
 * Autora: Daniela Morales
 * Descripción: Guarda los datos de las organizaciones registradas.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organizaciones")
public class Organizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "creada_en", insertable = false, updatable = false)
    private OffsetDateTime creadaEn;
}
