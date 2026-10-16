package pe.intiguard.inventario.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stock por producto. La PK es el id de ms-catalogo (sin FK: otro schema).
 * La BD rechaza valores negativos (CHECK): es la última barrera contra la sobreventa.
 */
@Entity
@Table(name = "stock")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stock {

    /** Asignado a mano: es el id del producto en ms-catalogo, no un autoincremental. */
    @Id
    @Column(name = "id_producto")
    private Long idProducto;

    /** Unidades que se pueden vender ahora mismo. */
    @Builder.Default
    @Column(nullable = false)
    private int disponible = 0;

    /** Unidades apartadas por un checkout en curso que todavía no se confirma. */
    @Builder.Default
    @Column(nullable = false)
    private int reservado = 0;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime actualizadoEn;
}
