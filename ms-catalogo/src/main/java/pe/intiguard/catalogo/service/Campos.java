package pe.intiguard.catalogo.service;

import java.math.BigDecimal;

/** Lectura tolerante de los valores de un cuerpo JSON recibido como Map. */
final class Campos {

    private Campos() {
    }

    /** Texto recortado, o null si no vino o vino vacío. */
    static String texto(Object valor) {
        if (valor == null) return null;
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    /** null si no es un número. Sin ceros de sobra a la derecha: 159.90 -> 159.9. */
    static BigDecimal decimal(Object valor) {
        if (valor == null) return null;
        try {
            BigDecimal numero = new BigDecimal(valor.toString().trim()).stripTrailingZeros();
            return numero.scale() < 0 ? numero.setScale(0) : numero;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** null si no es un entero. */
    static Long entero(Object valor) {
        if (valor instanceof Number n && n.doubleValue() == n.longValue()) return n.longValue();
        if (valor instanceof String s) {
            try {
                return Long.valueOf(s.trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }
}
