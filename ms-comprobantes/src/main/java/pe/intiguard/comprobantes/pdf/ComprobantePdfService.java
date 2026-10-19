package pe.intiguard.comprobantes.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.intiguard.comprobantes.client.PedidosClient;
import pe.intiguard.comprobantes.client.VentaDto;
import pe.intiguard.comprobantes.entity.Comprobante;
import pe.intiguard.comprobantes.entity.TipoComprobante;
import pe.intiguard.comprobantes.mapper.ComprobanteMapper;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF A4 con el mismo contenido que la vista 4.12: cabecera, datos del cliente, productos,
 * totales con el importe en letras y pie. Los productos se piden a ms-pedidos (GET /pedidos/{idVenta});
 * si no responde, el PDF sale igual con un aviso en lugar de la tabla.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComprobantePdfService {

    private static final Color AZUL = new Color(0x1E, 0x3A, 0x8A);
    private static final Color AZUL_SUAVE = new Color(0xE8, 0xEE, 0xFA);
    private static final Color GRIS = new Color(0x6B, 0x72, 0x80);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font EMPRESA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, AZUL);
    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, AZUL);
    private static final Font NUMERO = FontFactory.getFont(FontFactory.COURIER_BOLD, 13);
    private static final Font PIE = FontFactory.getFont(FontFactory.HELVETICA, 9, GRIS);

    private final PedidosClient pedidosClient;

    public byte[] generar(Comprobante c) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, salida);
        doc.open();
        boolean factura = c.getTipo() == TipoComprobante.FACTURA;

        doc.add(cabecera(c, factura));
        doc.add(datos(c, factura));
        doc.add(detalle(c));
        doc.add(totales(c));

        Paragraph letras = new Paragraph("Son: " + NumeroALetras.soles(c.getTotal()), NEGRITA);
        letras.setSpacingBefore(10);
        doc.add(letras);

        Paragraph pie = new Paragraph("Representación impresa de la " + (factura ? "factura" : "boleta de venta")
                + " electrónica.\nGracias por tu compra", PIE);
        pie.setAlignment(Element.ALIGN_CENTER);
        pie.setSpacingBefore(40);
        doc.add(pie);

        doc.close();
        return salida.toByteArray();
    }

    private PdfPTable cabecera(Comprobante c, boolean factura) {
        PdfPTable tabla = new PdfPTable(new float[]{3, 2});
        tabla.setWidthPercentage(100);

        PdfPCell empresa = new PdfPCell();
        empresa.setBorder(Rectangle.NO_BORDER);
        empresa.addElement(new Paragraph("IntiGuard S.A.C.", EMPRESA));
        empresa.addElement(new Paragraph("RUC 20512345678", NORMAL));
        empresa.addElement(new Paragraph("Av. Javier Prado Este 1234, San Isidro, Lima", NORMAL));
        tabla.addCell(empresa);

        PdfPCell recuadro = new PdfPCell();
        recuadro.setBorderColor(AZUL);
        recuadro.setBorderWidth(2);
        recuadro.setPadding(10);
        Paragraph titulo = new Paragraph(factura ? "FACTURA ELECTRÓNICA" : "BOLETA DE VENTA ELECTRÓNICA", TITULO);
        titulo.setAlignment(Element.ALIGN_CENTER);
        Paragraph numero = new Paragraph(ComprobanteMapper.numero(c), NUMERO);
        numero.setAlignment(Element.ALIGN_CENTER);
        recuadro.addElement(titulo);
        recuadro.addElement(numero);
        tabla.addCell(recuadro);
        tabla.setSpacingAfter(20);
        return tabla;
    }

    private PdfPTable datos(Comprobante c, boolean factura) {
        PdfPTable tabla = new PdfPTable(new float[]{1, 3});
        tabla.setWidthPercentage(100);
        fila(tabla, "Fecha de emisión", c.getEmitidoEn() == null ? "" : c.getEmitidoEn().format(FECHA));
        fila(tabla, factura ? "Razón social" : "Cliente", c.getNombreRazonSocial() == null ? "Cliente varios" : c.getNombreRazonSocial());
        if (c.getDocCliente() != null) {
            fila(tabla, factura ? "RUC" : "DNI", c.getDocCliente());
        }
        fila(tabla, "Pedido", "#%05d".formatted(c.getIdVenta()));
        tabla.setSpacingAfter(16);
        return tabla;
    }

    private Element detalle(Comprobante c) {
        List<VentaDto.Detalle> lineas = productos(c.getIdVenta());
        if (lineas.isEmpty()) {
            Paragraph aviso = new Paragraph("El detalle de productos no está disponible en este momento.", PIE);
            aviso.setSpacingAfter(10);
            return aviso;
        }
        PdfPTable tabla = new PdfPTable(new float[]{1, 5, 2, 2});
        tabla.setWidthPercentage(100);
        for (String titulo : List.of("Cant.", "Descripción", "P. unitario", "Importe")) {
            PdfPCell celda = new PdfPCell(new Phrase(titulo, NEGRITA));
            celda.setBackgroundColor(AZUL_SUAVE);
            celda.setPadding(6);
            celda.setBorderColor(AZUL_SUAVE);
            tabla.addCell(celda);
        }
        for (VentaDto.Detalle d : lineas) {
            tabla.addCell(celda(String.valueOf(d.cantidad()), Element.ALIGN_CENTER));
            tabla.addCell(celda(d.nombreProducto(), Element.ALIGN_LEFT));
            tabla.addCell(celda(soles(d.precioUnitario()), Element.ALIGN_RIGHT));
            BigDecimal importe = d.importe() != null ? d.importe() : d.precioUnitario().multiply(BigDecimal.valueOf(d.cantidad()));
            tabla.addCell(celda(soles(importe), Element.ALIGN_RIGHT));
        }
        tabla.setSpacingAfter(12);
        return tabla;
    }

    private PdfPTable totales(Comprobante c) {
        PdfPTable tabla = new PdfPTable(new float[]{4, 2, 2});
        tabla.setWidthPercentage(100);
        filaTotal(tabla, "Op. gravada", soles(c.getSubtotal()), NORMAL);
        filaTotal(tabla, "IGV (18 %)", soles(c.getIgv()), NORMAL);
        filaTotal(tabla, "Importe total", soles(c.getTotal()), NEGRITA);
        return tabla;
    }

    private List<VentaDto.Detalle> productos(Long idVenta) {
        try {
            VentaDto venta = pedidosClient.obtenerVenta(idVenta);
            return venta == null || venta.detalles() == null ? List.of() : venta.detalles();
        } catch (RuntimeException ex) {
            log.warn("No se pudo leer la venta {} en ms-pedidos para el PDF: {}", idVenta, ex.getMessage());
            return List.of();
        }
    }

    private static void fila(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell e = new PdfPCell(new Phrase(etiqueta, NEGRITA));
        e.setBorder(Rectangle.NO_BORDER);
        e.setPadding(3);
        PdfPCell v = new PdfPCell(new Phrase(valor, NORMAL));
        v.setBorder(Rectangle.NO_BORDER);
        v.setPadding(3);
        tabla.addCell(e);
        tabla.addCell(v);
    }

    private static void filaTotal(PdfPTable tabla, String etiqueta, String valor, Font fuente) {
        PdfPCell vacia = new PdfPCell();
        vacia.setBorder(Rectangle.NO_BORDER);
        tabla.addCell(vacia);
        PdfPCell e = new PdfPCell(new Phrase(etiqueta, fuente));
        e.setBorder(Rectangle.NO_BORDER);
        e.setPadding(3);
        tabla.addCell(e);
        PdfPCell v = new PdfPCell(new Phrase(valor, fuente));
        v.setBorder(Rectangle.NO_BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setPadding(3);
        tabla.addCell(v);
    }

    private static PdfPCell celda(String texto, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, NORMAL));
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(5);
        celda.setBorderColor(AZUL_SUAVE);
        return celda;
    }

    private static String soles(BigDecimal monto) {
        return "S/ " + monto.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
