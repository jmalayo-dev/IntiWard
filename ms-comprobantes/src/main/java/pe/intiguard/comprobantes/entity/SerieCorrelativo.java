package pe.intiguard.comprobantes.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Última numeración usada por serie (B001 boletas, F001 facturas).
 * Para que dos emisiones simultáneas no tomen el mismo número, la fila se lee
 * con bloqueo pesimista (SELECT ... FOR UPDATE): ver SerieCorrelativoRepository.findFirstByTipoOrderBySerieAsc.
 */
@Entity
@Table(name = "serie_correlativo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerieCorrelativo {

    /** B001, F001: la serie es la PK (la BD valida que B vaya con BOLETA y F con FACTURA). */
    @Id
    @Column(length = 4)
    private String serie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoComprobante tipo;

    @Builder.Default
    @Column(nullable = false)
    private int ultimoCorrelativo = 0;

    /** Avanza el correlativo y devuelve el nuevo. Llamar solo con la fila bloqueada. */
    public int siguiente() {
        return ++ultimoCorrelativo;
    }
}
