package pe.intiguard.comprobantes.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Importe en letras para el comprobante: 578.00 -> "QUINIENTOS SETENTA Y OCHO CON 00/100 SOLES". */
public final class NumeroALetras {

    private static final String[] UNIDADES = {"", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE",
            "VEINTE", "VEINTIUNO", "VEINTIDÓS", "VEINTITRÉS", "VEINTICUATRO", "VEINTICINCO", "VEINTISÉIS", "VEINTISIETE",
            "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] DECENAS = {"", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA",
            "OCHENTA", "NOVENTA"};
    private static final String[] CENTENAS = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    private NumeroALetras() {
    }

    public static String soles(BigDecimal monto) {
        BigDecimal redondeado = monto.setScale(2, RoundingMode.HALF_UP);
        long entero = redondeado.longValue();
        int centimos = redondeado.subtract(BigDecimal.valueOf(entero)).movePointRight(2).intValue();
        return "%s CON %02d/100 SOLES".formatted(entero == 0 ? "CERO" : letras(entero), centimos);
    }

    static String letras(long n) {
        if (n >= 1_000_000) {
            long millones = n / 1_000_000;
            String prefijo = millones == 1 ? "UN MILLÓN" : apocopar(letras(millones)) + " MILLONES";
            long resto = n % 1_000_000;
            return resto == 0 ? prefijo : prefijo + " " + letras(resto);
        }
        if (n >= 1000) {
            long miles = n / 1000;
            String prefijo = miles == 1 ? "MIL" : apocopar(letras(miles)) + " MIL";
            long resto = n % 1000;
            return resto == 0 ? prefijo : prefijo + " " + letras(resto);
        }
        return menorQueMil((int) n);
    }

    private static String menorQueMil(int n) {
        if (n == 100) return "CIEN";
        String centena = CENTENAS[n / 100];
        int resto = n % 100;
        String decena = resto < 30 ? UNIDADES[resto]
                : DECENAS[resto / 10] + (resto % 10 == 0 ? "" : " Y " + UNIDADES[resto % 10]);
        if (centena.isEmpty()) return decena;
        return decena.isEmpty() ? centena : centena + " " + decena;
    }

    /** "VEINTIUNO MIL" -> "VEINTIÚN MIL", "UNO" -> "UN" delante de MIL o MILLONES. */
    private static String apocopar(String texto) {
        if (texto.endsWith("VEINTIUNO")) return texto.substring(0, texto.length() - 9) + "VEINTIÚN";
        if (texto.endsWith("UNO")) return texto.substring(0, texto.length() - 3) + "UN";
        return texto;
    }
}
