package pe.intiguard.pedidos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venta")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long idUsuario;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVenta estado = EstadoVenta.PENDIENTE;

    /** Id del comprobante emitido por ms-comprobantes. */
    private Long idComprobante;

    @Builder.Default
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();
}
