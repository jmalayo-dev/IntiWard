package pe.intiguard.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Datos comerciales del producto. La cantidad en stock NO vive aquí:
 * es de ms-inventario (ver "Qué cambia respecto al modelo del informe").
 */
@Entity
@Table(name = "producto")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    /** URL pública en S3/CloudFront (semana 8). */
    @Column(length = 300)
    private String imagenUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_marca")
    private Marca marca;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
