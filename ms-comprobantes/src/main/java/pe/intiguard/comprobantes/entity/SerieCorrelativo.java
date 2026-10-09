package pe.intiguard.comprobantes.entity;

import jakarta.persistence.*;
import lombok.*;

/** Última numeración usada por serie: B001 para boletas, F001 para facturas. */
@Entity
@Table(name = "serie_correlativo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerieCorrelativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private TipoComprobante tipo;

    @Column(nullable = false, length = 4)
    private String serie;

    @Column(nullable = false)
    private long ultimoCorrelativo;

    /** Evita que dos emisiones simultáneas tomen el mismo número. */
    @Version
    private Long version;
}
