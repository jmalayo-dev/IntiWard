package pe.intiguard.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rol")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** ADMIN o CLIENTE */
    @Column(nullable = false, unique = true, length = 30)
    private String nombre;
}
