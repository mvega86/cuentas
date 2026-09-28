package mvega.dev.cuentas.receipt.importer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Fixtures de tickets de Mercadona, en texto y anonimizados.
 *
 * <p>Son el texto que PDFBox extrae de los ocho tickets reales, con los datos personales
 * sustituidos: dirección y teléfono de la tienda, últimos cuatro dígitos de la tarjeta,
 * código de autorización, entidad y número de operación. Se conservan las líneas de
 * producto y los importes, que es lo que el parser tiene que interpretar.
 *
 * <p>Los PDFs reales <strong>no se versionan</strong>: contienen el historial de compra de
 * una persona. Para las pruebas que necesitan un PDF de verdad, {@link #pdfOf(String)}
 * fabrica uno con PDFBox a partir del fixture, así que el camino PDF -> texto -> parser se
 * ejercita de punta a punta sin publicar datos de nadie.
 *
 * <p>Los nombres de los fixtures no llevan ni fecha de compra fiable ni importe: el parser
 * saca todo del contenido y nunca del nombre del fichero (CLAUDE §6).
 */
final class TicketFixtures {

    static final String TICKET_3_77 = "ticket-2026-09-12-a.txt";
    static final String TICKET_17_72 = "ticket-2026-09-14-a.txt";
    static final String TICKET_33_63 = "ticket-2026-09-16-a.txt";
    static final String TICKET_3_68 = "ticket-2026-09-18-a.txt";
    static final String TICKET_48_75 = "ticket-2026-09-19-a.txt";
    static final String TICKET_19_00 = "ticket-2026-09-19-b.txt";
    static final String TICKET_2_45 = "ticket-2026-09-22-a.txt";
    static final String TICKET_73_70 = "ticket-2026-09-26-a.txt";

    /** Los ocho tickets. El parser debe cubrirlos todos (CLAUDE §12). */
    static final List<String> ALL = List.of(TICKET_3_77, TICKET_17_72, TICKET_33_63, TICKET_3_68,
            TICKET_48_75, TICKET_19_00, TICKET_2_45, TICKET_73_70);

    private TicketFixtures() {
    }

    /** El texto del ticket, tal y como lo devolvería PDFBox. */
    static String textOf(String fixture) {
        String path = "/fixtures/mercadona/" + fixture;
        try (InputStream in = TicketFixtures.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Fixture no encontrado en classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Un PDF de una página con el texto del fixture, generado al vuelo con PDFBox. */
    static byte[] pdfOf(String fixture) {
        return pdfFromText(textOf(fixture));
    }

    static byte[] pdfFromText(String text) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            var font = new PDType1Font(Standard14Fonts.FontName.COURIER);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setFont(font, 8f);
                content.beginText();
                content.setLeading(9.5f);
                content.newLineAtOffset(40, page.getMediaBox().getHeight() - 40);
                for (String line : text.split("\\R")) {
                    // showText no admite saltos de línea, de ahí una llamada por línea.
                    content.showText(line);
                    content.newLine();
                }
                content.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Un PDF de otro comercio, con la misma pinta que un ticket: tiene FECHA, IVA, TOTAL e
     * importes. Sirve para comprobar que esas palabras genéricas no bastan para que se
     * interprete como un gasto.
     */
    static byte[] otherMerchantPdf() {
        return pdfFromText("""
                SUPERMERCADOS EJEMPLO, S.L.   B-00000000
                CL OTRA, 2
                00000 CIUDAD
                FECHA: 26/09/2026 10:00
                Cnt. Descripción P. Unit Importe
                1 PRODUCTO CUALQUIERA 9,99
                2 OTRO PRODUCTO 1,00 2,00
                TOTAL (€) 11,99
                TARJETA BANCARIA 11,99
                IVA BASE IMP. (€) CUOTA (€) TOTAL (€)
                21% 9,91 2,08 11,99
                """);
    }

    /**
     * Un PDF que menciona Mercadona pero no es un ticket: un apunte de extracto bancario.
     * Es el caso que el reconocimiento anterior dejaba pasar, porque solo buscaba la palabra.
     */
    static byte[] bankStatementMentioningMercadonaPdf() {
        return pdfFromText("""
                BANCO EJEMPLO - EXTRACTO DE CUENTA
                FECHA        CONCEPTO                      IMPORTE
                12/09/2026   COMPRA MERCADONA                 3,77
                14/09/2026   COMPRA MERCADONA                17,72
                26/09/2026   COMPRA MERCADONA, S.A.          73,70
                TOTAL (€) 95,19
                """);
    }

    /** Un fichero que no es un PDF en absoluto. */
    static byte[] notAPdf() {
        return "esto no es un PDF, es texto suelto".getBytes(StandardCharsets.UTF_8);
    }
}
