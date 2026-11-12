
package cr.petmatchcr.acceso.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario {
    /**
     * Versión 01
     * Autora: Daniela Morales
     *
     * Descripción: Esta clase representa a los usuarios registrados en la aplicación.
     *              Contiene información personal, el rol y el estado de la cuenta.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identidad estable compartida con B y C; el Long sigue siendo interno de A.
    @Column(name = "id_publico", nullable = false, unique = true, updatable = false)
    private UUID idPublico = UUID.randomUUID();

    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_desactivacion")
    private LocalDateTime fechaDesactivacion;

    @Column(name = "fecha_reactivacion")
    private LocalDateTime fechaReactivacion;
}
