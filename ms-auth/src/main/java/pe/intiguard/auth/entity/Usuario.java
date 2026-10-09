package pe.intiguard.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    /** Hash BCrypt. Nunca se devuelve en una respuesta ni se escribe en el log. */
    @Column(nullable = false, length = 100)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rol")
    private Rol rol;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    /** Para el bloqueo temporal: 5 intentos fallidos bloquean 15 minutos. */
    @Builder.Default
    @Column(nullable = false)
    private int intentosFallidos = 0;

    private LocalDateTime bloqueadoHasta;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
