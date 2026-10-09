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
    private Integer id;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;
}
