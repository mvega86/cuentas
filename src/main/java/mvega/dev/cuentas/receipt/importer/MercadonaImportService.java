package mvega.dev.cuentas.receipt.importer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mvega.dev.cuentas.receipt.ParsingStatus;
import mvega.dev.cuentas.receipt.Receipt;
import mvega.dev.cuentas.receipt.ReceiptRepository;
import mvega.dev.cuentas.receipt.SourceType;
import mvega.dev.cuentas.shared.error.ApiException;
import mvega.dev.cuentas.shared.text.ContentHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Pipeline único de importación de tickets de Mercadona.
 *
 * <p>Tanto la carpeta automática como la subida manual terminan aquí: no hay dos
 * implementaciones. El canal solo cambia de dónde salen los bytes y si hay un fichero que
 * mover.
 *
 * <p>Pasos por cada PDF:
 * <ol>
 *   <li>SHA-256 del contenido, antes de procesar nada.</li>
 *   <li>Registro del intento, para que quede rastro pase lo que pase.</li>
 *   <li>Descarte si ese hash ya se procesó: {@code DUPLICATE}, sin generar otro gasto.</li>
 *   <li>Extracción del texto e interpretación con el parser.</li>
 *   <li>Persistencia del ticket y de sus líneas.</li>
 *   <li>El fichero va a {@code procesados} si salió bien, o a {@code error} si no.</li>
 * </ol>
 *
 * <p>El parser no sabe nada de esto: solo interpreta texto. No mueve ficheros, no accede a
 * Gmail ni a Drive y no escribe en la base de datos.
 */
@Service
public class MercadonaImportService {

    private static final Logger log = LoggerFactory.getLogger(MercadonaImportService.class);

    private final PdfTextExtractor pdfTextExtractor;
    private final List<ReceiptParser> parsers;
    private final ReceiptRepository receiptRepository;
    private final ReceiptWriter receiptWriter;
    private final ImportRecordService importRecordService;
    private final ImportFolders folders;

    MercadonaImportService(PdfTextExtractor pdfTextExtractor, List<ReceiptParser> parsers,
            ReceiptRepository receiptRepository, ReceiptWriter receiptWriter,
            ImportRecordService importRecordService, ImportFolders folders) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.parsers = parsers;
        this.receiptRepository = receiptRepository;
        this.receiptWriter = receiptWriter;
        this.importRecordService = importRecordService;
        this.folders = folders;
    }

    // ------------------------------------------------------------------ carpeta automática

    /**
     * Revisa la carpeta de entrada e importa los PDFs que haya.
     *
     * <p>Es idempotente: el proceso periódico puede ejecutarla cada pocos segundos sin
     * duplicar nada.
     *
     * @throws ApiException si la carpeta no está configurada o no se puede leer
     */
    public List<ImportRecord> scanInbox() {
        ImportFolders.FolderState state = folders.state();
        if (!state.configured()) {
            throw new ApiException(
                    "La carpeta de entrada no está configurada. Define la variable de entorno "
                            + "CUENTAS_IMPORT_MERCADONA_DIR.",
                    HttpStatus.CONFLICT);
        }
        if (!state.readable()) {
            throw new ApiException(state.message(), HttpStatus.CONFLICT);
        }

        List<Path> pdfs;
        try {
            pdfs = folders.pendingPdfs();
        } catch (IOException e) {
            throw new ApiException("No se pudo leer la carpeta de entrada: " + e.getMessage(),
                    HttpStatus.CONFLICT);
        }

        if (pdfs.isEmpty()) {
            return List.of();
        }
        log.info("Carpeta {}: {} PDFs por procesar", state.path(), pdfs.size());

        List<ImportRecord> records = new ArrayList<>(pdfs.size());
        for (Path pdf : pdfs) {
            records.add(importFromInbox(pdf));
        }
        return records;
    }

    private ImportRecord importFromInbox(Path pdf) {
        String filename = pdf.getFileName().toString();
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(pdf);
        } catch (IOException e) {
            // Puede estar a medio sincronizar: se deja donde está y se reintenta en la
            // pasada siguiente, en lugar de mandarlo a error.
            log.warn("No se pudo leer {}, se reintentará", pdf, e);
            ImportRecord record = importRecordService.start(ImportSource.MERCADONA, filename,
                    "sin-hash-" + Instant.now().toEpochMilli());
            return importRecordService.markError(record.getId(),
                    "No se pudo leer el fichero, se reintentará en la próxima pasada: "
                            + e.getMessage(),
                    null);
        }
        return process(bytes, filename, SourceType.FOLDER, pdf);
    }

    // -------------------------------------------------------------------- subida manual

    /**
     * Importa un PDF subido a mano. Pasa por el mismo pipeline que la carpeta.
     *
     * <p>El fichero no viene de la carpeta de entrada, así que se guarda una copia en
     * {@code procesados} o en {@code error} según el resultado, para no perder el original.
     */
    public ImportRecord importUpload(byte[] bytes, String originalFilename) {
        String filename = originalFilename == null || originalFilename.isBlank()
                ? "subida-manual.pdf"
                : originalFilename;
        return process(bytes, filename, SourceType.UPLOAD, null);
    }

    // ------------------------------------------------------------------------- pipeline

    private ImportRecord process(byte[] bytes, String filename, SourceType channel,
            Path sourceFile) {

        String sha256 = ContentHasher.sha256(bytes);
        ImportRecord record = importRecordService.start(ImportSource.MERCADONA, filename, sha256);
        importRecordService.markProcessing(record.getId());

        Optional<Receipt> alreadyImported = receiptRepository.findByContentHash(sha256);
        if (alreadyImported.isPresent()) {
            Path stored = settle(bytes, filename, sourceFile, true);
            log.info("{} ya estaba importado, se marca DUPLICATE", filename);
            return importRecordService.markDuplicate(record.getId(), alreadyImported.get(),
                    "Este PDF ya estaba importado (mismo contenido)", stored);
        }

        String rawText;
        try {
            rawText = pdfTextExtractor.extract(bytes);
        } catch (PdfTextExtractor.PdfExtractionException e) {
            return fail(record, bytes, filename, sourceFile,
                    "El fichero no se pudo leer como PDF: " + e.getMessage());
        }

        // Que el PDF esté en la carpeta de Mercadona no garantiza que sea un ticket de
        // Mercadona. Si ningún parser lo reconoce, el documento no se interpreta en absoluto:
        // no se crea ticket, no se crean líneas y no se contabiliza ningún gasto.
        Optional<ReceiptParser> parser = parsers.stream()
                .filter(candidate -> candidate.supports(rawText))
                .findFirst();
        if (parser.isEmpty()) {
            return unsupported(record, bytes, filename, sourceFile,
                    "Documento no reconocido como ticket de Mercadona");
        }

        ParsedReceipt parsed = parser.get().parse(rawText);
        if (parsed.status() == ParsingStatus.ERROR) {
            String detail = parsed.warnings().isEmpty()
                    ? "no se encontraron la fecha, el total ni las líneas"
                    : String.join("; ", parsed.warnings());
            return fail(record, bytes, filename, sourceFile,
                    "No se pudieron extraer los datos mínimos del ticket: " + detail);
        }

        Receipt saved;
        try {
            saved = receiptWriter.write(parsed, sha256, filename, channel,
                    parser.get().parserVersion());
        } catch (ReceiptWriter.DuplicateInvoiceException e) {
            // Mismo ticket, fichero distinto: por ejemplo el PDF se volvió a descargar. No
            // es un error, es un duplicado, y no debe generar otro gasto.
            Path stored = settle(bytes, filename, sourceFile, true);
            return importRecordService.markDuplicate(record.getId(),
                    receiptRepository.findById(e.getExistingReceiptId()).orElse(null),
                    e.getMessage(), stored);
        } catch (RuntimeException e) {
            log.error("Fallo al guardar el ticket de {}", filename, e);
            return fail(record, bytes, filename, sourceFile,
                    "No se pudo guardar el ticket: " + e.getMessage());
        }

        Path stored = settle(bytes, filename, sourceFile, true);
        log.info("Ticket importado {} desde {} ({} líneas, estado {})", saved.getId(), filename,
                saved.getItems().size(), saved.getParsingStatus());
        return importRecordService.markProcessed(record.getId(), saved, stored);
    }

    private ImportRecord fail(ImportRecord record, byte[] bytes, String filename, Path sourceFile,
            String message) {
        Path stored = settle(bytes, filename, sourceFile, false);
        log.warn("Importación fallida de {}: {}", filename, message);
        return importRecordService.markError(record.getId(), message, stored);
    }

    /**
     * Documento legible pero ajeno. Se registra y se aparta, sin crear nada.
     *
     * <p>Distinto de {@link #fail}: no hay ningún fallo que arreglar, solo un documento que
     * esta aplicación no sabe interpretar.
     */
    private ImportRecord unsupported(ImportRecord record, byte[] bytes, String filename,
            Path sourceFile, String message) {
        Path stored = settle(bytes, filename, sourceFile, false);
        log.info("{} no se reconoce como ticket de Mercadona, se aparta sin importar", filename);
        return importRecordService.markUnsupported(record.getId(), message, stored);
    }

    /** Deja el fichero donde toca: procesados si fue bien, error si no. */
    private Path settle(byte[] bytes, String filename, Path sourceFile, boolean ok) {
        Path targetDir = ok ? folders.processed() : folders.failed();
        if (sourceFile != null) {
            return ok ? folders.moveToProcessed(sourceFile) : folders.moveToFailed(sourceFile);
        }
        // Subida manual: no hay fichero de origen, así que se guarda una copia.
        return folders.store(bytes, filename, targetDir);
    }

}
