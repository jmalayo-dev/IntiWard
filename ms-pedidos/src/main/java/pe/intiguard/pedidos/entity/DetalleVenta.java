package pe.intiguard.pedidos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_venta")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_venta")
    private Venta venta;

    @Column(nullable = false)
    private Long idProducto;

    /** Se copia el nombre al momento de la venta: si el catálogo cambia, el pedido no. */
    @Column(nullable = false, length = 150)
    private String nombreProducto;

    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
}
