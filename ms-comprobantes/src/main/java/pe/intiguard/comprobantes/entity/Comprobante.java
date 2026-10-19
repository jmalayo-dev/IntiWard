package pe.intiguard.comprobantes.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Comprobante de una venta. La BD valida el documento del cliente:
 * FACTURA exige RUC (11 dígitos) y razón social; BOLETA admite DNI (8 dígitos) o nada.
 */
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
    @Column(name = "id_comprobante")
    private Long id;

    /** Id de la venta en ms-pedidos. Una venta, un comprobante. */
    @Column(nullable = false, unique = true)
    private Long idVenta;

    /** Dueño de la venta (id en ms-auth). Lo manda ms-pedidos; sirve para el control de dueño y no sale en la respuesta. */
    @Column(nullable = false)
    private Long idUsuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoComprobante tipo;

    @Column(nullable = false, length = 4)
    private String serie;

    @Column(nullable = false)
    private int correlativo;

    /**
     * B001-00000001: lo calcula MySQL (columna GENERATED) a partir de serie y correlativo. No se relee tras
     * insertar (un viaje menos a la base); ComprobanteMapper.numero lo arma si todavía no se leyó.
     */
    @Column(length = 13, insertable = false, updatable = false)
    private String numero;

    /** DNI (boleta, opcional) o RUC (factura). */
    @Column(length = 11)
    private String docCliente;

    @Column(length = 150)
    private String nombreRazonSocial;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal igv;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    /** Vacía: el PDF se genera al pedirlo en GET /comprobantes/{id}/pdf. */
    @Column(length = 255)
    private String pdfUrl;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime emitidoEn;
}
