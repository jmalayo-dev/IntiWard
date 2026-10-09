package pe.intiguard.inventario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Historial auditable: es lo que hace medible el Objetivo 2 del informe. */
@Entity
@Table(name = "movimiento_stock")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long idProducto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private int cantidad;

    /** Por ejemplo el id de la venta que generó el movimiento. */
    @Column(length = 60)
    private String referencia;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();
}
