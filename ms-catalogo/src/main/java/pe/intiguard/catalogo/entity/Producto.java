package pe.intiguard.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @Column(name = "id_producto")
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_marca")
    private Marca marca;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;

    /** Incluye IGV (CHECK precio > 0 en la BD). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    /** URL pública en S3/CloudFront (semana 8). */
    @Column(length = 255)
    private String imagenUrl;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    private LocalDateTime actualizadoEn;

    /** Se cargan por lotes al armar una página: una consulta para todos los productos, no una por producto. */
    @Builder.Default
    @BatchSize(size = 100)
    @OrderBy("orden ASC")
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductoEspecificacion> especificaciones = new ArrayList<>();
}
