package mvega.dev.cuentas.receipt.importer;

import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Extrae el texto plano de un PDF con PDFBox.
 *
 * <p>Unica responsabilidad: PDF a texto. La interpretacion del contenido es cosa de los
 * {@link ReceiptParser}, para que la extraccion y el parseo de negocio queden separados
 * (CLAUDE §6). Los tickets de Mercadona llevan capa de texto real, asi que no hace
 * falta OCR.
 */
@Component
public class PdfTextExtractor {

    /**
     * @param pdfBytes contenido completo del PDF
     * @return el texto del documento, ordenado por posicion en la pagina
     * @throws PdfExtractionException si el PDF no se puede abrir o leer
     */
    public String extract(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            // Sin esto, PDFBox devuelve el texto en el orden en que esta dentro del
            // fichero, que no tiene por que ser el orden visual del ticket.
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (IOException e) {
            throw new PdfExtractionException("No se pudo extraer el texto del PDF", e);
        }
    }

    /** Fallo al leer el PDF, distinto de un fallo al interpretar su contenido. */
    public static class PdfExtractionException extends RuntimeException {
        public PdfExtractionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
