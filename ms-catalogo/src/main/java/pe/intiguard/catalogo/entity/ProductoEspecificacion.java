package pe.intiguard.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;

/** Pestaña "Especificaciones" del detalle: nombre y valor, en el orden en que se cargaron. */
@Entity
@Table(name = "producto_especificacion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoEspecificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especificacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto")
    private Producto producto;

    /** Único por producto (sin distinguir mayúsculas, por la collation de la base). */
    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String valor;

    @Builder.Default
    @Column(nullable = false)
    private int orden = 0;
}
