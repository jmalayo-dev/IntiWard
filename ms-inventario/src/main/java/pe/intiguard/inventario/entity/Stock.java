package pe.intiguard.inventario.entity;

import jakarta.persistence.*;
import lombok.*;

/** Cantidad disponible por producto. idProducto es el id de ms-catalogo (sin FK: otro schema). */
@Entity
@Table(name = "stock")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long idProducto;

    @Column(nullable = false)
    private int cantidad;

    /** Unidades apartadas por un checkout en curso que todavía no se confirma. */
    @Builder.Default
    @Column(nullable = false)
    private int cantidadReservada = 0;

    public int getDisponible() {
        return cantidad - cantidadReservada;
    }
}
