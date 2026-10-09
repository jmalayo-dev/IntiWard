package pe.intiguard.pedidos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** El carrito se persiste para sobrevivir a reinicios (en el informe vivía en la sesión). */
@Entity
@Table(name = "carrito")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id del usuario en ms-auth. Un carrito por usuario. */
    @Column(nullable = false, unique = true)
    private Long idUsuario;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Builder.Default
    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCarrito> items = new ArrayList<>();
}
