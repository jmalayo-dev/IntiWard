package pe.intiguard.pedidos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Montos: total = suma de precio x cantidad (los precios incluyen IGV),
 * subtotal = total / 1.18 e igv = total - subtotal. La BD exige total = subtotal + igv.
 * El comprobante emitido se busca en ms-comprobantes por idVenta.
 */
@Entity
@Table(name = "venta")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Venta {

    public static final BigDecimal FACTOR_IGV = new BigDecimal("1.18");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Long id;

    @Column(nullable = false)
    private Long idUsuario;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVenta estado = EstadoVenta.PENDIENTE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoComprobante tipoComprobante = TipoComprobante.BOLETA;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal igv;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaVenta;

    @UpdateTimestamp
    private LocalDateTime actualizadoEn;

    @Builder.Default
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    /** Fija total, subtotal e igv a partir del total con IGV incluido. */
    public void asignarMontos(BigDecimal totalConIgv) {
        this.total = totalConIgv.setScale(2, RoundingMode.HALF_UP);
        this.subtotal = total.divide(FACTOR_IGV, 2, RoundingMode.HALF_UP);
        this.igv = total.subtract(subtotal);
    }
}
