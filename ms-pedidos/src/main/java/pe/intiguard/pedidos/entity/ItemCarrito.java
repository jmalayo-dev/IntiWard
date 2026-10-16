package pe.intiguard.pedidos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "item_carrito")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_carrito")
    private Carrito carrito;

    /** Id del producto en ms-catalogo. Uno por carrito (si se repite, se suma la cantidad). */
    @Column(nullable = false)
    private Long idProducto;

    @Column(nullable = false)
    private int cantidad;
}
