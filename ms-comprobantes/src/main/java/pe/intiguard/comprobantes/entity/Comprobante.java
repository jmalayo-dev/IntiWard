package pe.intiguard.comprobantes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "comprobante")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id de la venta en ms-pedidos. Una venta, un comprobante. */
    @Column(nullable = false, unique = true)
    private Long idVenta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoComprobante tipo;

    /** Formato B001-00000001 / F001-00000001. */
    @Column(nullable = false, unique = true, length = 13)
    private String numero;

    /** DNI para boleta, RUC para factura. */
    @Column(length = 11)
    private String documentoCliente;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime fechaEmision = LocalDateTime.now();
}
