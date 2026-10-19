package pe.intiguard.pedidos.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Copia local de lo que devuelve ms-catalogo. Los servicios no comparten clases de dominio. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductoDto(Long id, String nombre, String marca, BigDecimal precio, String imagenUrl, Boolean activo) {

    /** Contrato C5: un producto inactivo responde 200 con activo: false. */
    public boolean estaActivo() {
        return activo == null || activo;
    }
}
