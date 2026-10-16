package pe.intiguard.inventario.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Historial auditable: es lo que hace medible el Objetivo 2 del informe.
 * Cantidad siempre positiva salvo en AJUSTE; el tipo define el efecto (ver 03-inventario.sql).
 */
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
    @Column(name = "id_movimiento")
    private Long id;

    @Column(nullable = false)
    private Long idProducto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private int cantidad;

    /** Venta que generó el movimiento. Obligatorio salvo en ENTRADA y AJUSTE. */
    private Long idVenta;

    /** Obligatorio en AJUSTE. */
    @Column(length = 200)
    private String motivo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
