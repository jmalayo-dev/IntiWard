package pe.intiguard.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "marca")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Marca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_marca")
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
