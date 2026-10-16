package pe.intiguard.inventario.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Unidades apartadas para una venta en curso. Una reserva ACTIVA que pasa su
 * expiraEn (15 min) la libera la tarea de vencimiento (CU09).
 */
@Entity
@Table(name = "reserva")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Long id;

    /** Id de la venta en ms-pedidos. Una reserva por venta y producto. */
    @Column(nullable = false)
    private Long idVenta;

    @Column(nullable = false)
    private Long idProducto;

    @Column(nullable = false)
    private int cantidad;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado = EstadoReserva.ACTIVA;

    @Column(nullable = false)
    private LocalDateTime expiraEn;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    private LocalDateTime actualizadoEn;
}
