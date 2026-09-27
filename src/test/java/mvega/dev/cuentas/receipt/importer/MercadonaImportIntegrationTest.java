package mvega.dev.cuentas.receipt.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Comparator;
import mvega.dev.cuentas.PostgresIntegrationTest;
import mvega.dev.cuentas.analytics.AnalyticsService;
import mvega.dev.cuentas.catalog.CategoryRepository;
import mvega.dev.cuentas.receipt.ParsingStatus;
import mvega.dev.cuentas.receipt.Receipt;
import mvega.dev.cuentas.receipt.ReceiptRepository;
import mvega.dev.cuentas.receipt.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * El pipeline completo contra PostgreSQL real: carpeta de entrada, subida manual, estados,
 * movimiento de ficheros y agregados del dashboard (CLAUDE §12, §15).
 *
 * <p>Los PDFs se fabrican al vuelo desde los fixtures anonimizados, así que se ejercita el
 * camino de verdad (PDF -> PDFBox -> parser -> base de datos) sin versionar tickets reales.
 */
class MercadonaImportIntegrationTest extends PostgresIntegrationTest {

    static final Path BASE =
            Path.of(System.getProperty("java.io.tmpdir"), "cuentas-import-it");
    static final Path INBOX = BASE.resolve("entrada");
    static final Path PROCESSED = BASE.resolve("procesados");
    static final Path FAILED = BASE.resolve("error");

    @DynamicPropertySource
    static void importProperties(DynamicPropertyRegistry registry) {
        registry.add("cuentas.imports.mercadona.directory", INBOX::toString);
        registry.add("cuentas.imports.mercadona.processed-directory", PROCESSED::toString);
        registry.add("cuentas.imports.mercadona.error-directory", FAILED::toString);
        // El sondeo periódico no debe dispararse a media prueba.
        registry.add("cuentas.imports.scan-delay-ms", () -> 3_600_000);
    }

    @Autowired
    private MercadonaImportService importService;
    @Autowired
    private MercadonaFolderScanner scanner;
    @Autowired
    private ImportQueryService queryService;
    @Autowired
    private ImportRecordRepository importRecordRepository;
    @Autowired
    private ReceiptRepository receiptRepository;
    @Autowired
    private mvega.dev.cuentas.receipt.ReceiptService receiptService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AnalyticsService analyticsService;
    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void reset() throws IOException {
        jdbcClient.sql("delete from import_record").update();
        jdbcClient.sql("truncate table receipt cascade").update();
        jdbcClient.sql("delete from product_rule").update();
        for (Path dir : List.of(INBOX, PROCESSED, FAILED)) {
            Files.createDirectories(dir);
            emptyFolder(dir);
        }
    }

    private void emptyFolder(Path dir) throws IOException {
        try (var entries = Files.list(dir)) {
            entries.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    private void dropInInbox(String fixture) {
        try {
            Files.write(INBOX.resolve(fixture.replace(".txt", ".pdf")),
                    TicketFixtures.pdfOf(fixture));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<String> namesIn(Path dir) throws IOException {
        try (var entries = Files.list(dir)) {
            return entries.map(path -> path.getFileName().toString()).sorted().toList();
        }
    }

    // ------------------------------------------------------------------------- esquema

    @Test
    @DisplayName("las migraciones crean el registro de importaciones y el catálogo acordado")
    void migrationsCreateSchemaAndCatalog() {
        List<String> tables = jdbcClient.sql("""
                select table_name from information_schema.tables
                where table_schema = 'public' order by table_name
                """).query(String.class).list();

        assertThat(tables).contains("category", "import_record", "merchant", "product_rule",
                "receipt", "receipt_item", "receipt_tax_line");
        assertThat(categoryRepository.findAllByOrderByNameAsc())
                .extracting("name")
                .containsExactlyInAnyOrder("Alimentación", "Bebidas", "Higiene personal",
                        "Hogar", "Limpieza", "Mascotas", "Otros");
    }

    // --------------------------------------------------------------- carpeta automática

    @Test
    @DisplayName("los ocho tickets de la carpeta se importan y quedan en PROCESSED")
    void scanImportsEveryTicketFromTheInbox() {
        TicketFixtures.ALL.forEach(this::dropInInbox);

        List<ImportRecord> records = scanner.scanNow();

        assertThat(records).hasSize(8);
        assertThat(records).allSatisfy(record -> {
            assertThat(record.getProcessingStatus()).isEqualTo(ImportStatus.PROCESSED);
            assertThat(record.getSource()).isEqualTo(ImportSource.MERCADONA);
            assertThat(record.getErrorMessage()).isNull();
            assertThat(record.getSha256()).hasSize(64);
        });
        assertThat(receiptRepository.count()).isEqualTo(8);
        assertThat(receiptRepository.findAll())
                .extracting(Receipt::getSourceType)
                .containsOnly(SourceType.FOLDER);
        assertThat(receiptRepository.findAll())
                .extracting(Receipt::getParsingStatus)
                .containsOnly(ParsingStatus.PARSED);
    }

    @Test
    @DisplayName("un ticket procesado sale de entrada y acaba en procesados")
    void processedFilesMoveToProcessedFolder() throws IOException {
        dropInInbox(TicketFixtures.TICKET_73_70);

        scanner.scanNow();

        assertThat(namesIn(INBOX)).isEmpty();
        assertThat(namesIn(PROCESSED)).containsExactly("ticket-2026-09-26-a.pdf");
        assertThat(namesIn(FAILED)).isEmpty();
        assertThat(importRecordRepository.findAll()).singleElement()
                .satisfies(record -> assertThat(record.getStoredPath())
                        .contains("procesados"));
    }

    @Test
    @DisplayName("una segunda pasada no encuentra nada, porque la entrada quedó vacía")
    void secondScanFindsNothing() {
        TicketFixtures.ALL.forEach(this::dropInInbox);
        scanner.scanNow();

        assertThat(scanner.scanNow()).isEmpty();
        assertThat(receiptRepository.count()).isEqualTo(8);
    }

    @Test
    @DisplayName("el mismo fichero byte a byte es DUPLICATE por el hash del contenido")
    void identicalBytesAreDuplicatedByHash() throws IOException {
        // El mismo array de bytes las dos veces: es la primera capa antiduplicado.
        byte[] pdf = TicketFixtures.pdfOf(TicketFixtures.TICKET_73_70);
        Files.write(INBOX.resolve("primera.pdf"), pdf);
        scanner.scanNow();

        Files.write(INBOX.resolve("segunda.pdf"), pdf);
        List<ImportRecord> second = scanner.scanNow();

        assertThat(second).singleElement().satisfies(record -> {
            assertThat(record.getProcessingStatus()).isEqualTo(ImportStatus.DUPLICATE);
            assertThat(record.getErrorMessage()).contains("ya estaba importado");
            // Apunta al ticket que ya existía, para poder abrirlo desde el historial.
            assertThat(record.getReceipt()).isNotNull();
        });
        assertThat(receiptRepository.count()).isEqualTo(1);
        assertThat(importRecordRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("el mismo ticket en otro fichero es DUPLICATE por el número de factura")
    void sameTicketInADifferentFileIsDuplicatedByInvoice() {
        dropInInbox(TicketFixtures.TICKET_73_70);
        scanner.scanNow();
        BigDecimal totalAfterFirst = receiptRepository.findAll().getFirst().getTotalAmount();

        // PDFBox mete la fecha de creación y un identificador de documento, así que dos
        // generaciones del mismo texto NO dan los mismos bytes: el hash no lo caza y entra
        // en juego la segunda capa, la factura. Es justo lo que pasaría si se volviera a
        // descargar el adjunto y el PDF no fuera idéntico.
        dropInInbox(TicketFixtures.TICKET_73_70);
        List<ImportRecord> second = scanner.scanNow();

        assertThat(second).singleElement().satisfies(record -> {
            assertThat(record.getProcessingStatus()).isEqualTo(ImportStatus.DUPLICATE);
            assertThat(record.getErrorMessage()).contains("factura 2345-012-184052");
            assertThat(record.getReceipt()).isNotNull();
        });
        assertThat(receiptRepository.count()).isEqualTo(1);
        assertThat(receiptRepository.findAll().getFirst().getTotalAmount())
                .isEqualByComparingTo(totalAfterFirst);
    }

    @Test
    @DisplayName("un PDF ilegible acaba en ERROR, con el motivo y movido a error/")
    void unreadableFileEndsUpInErrorFolder() throws IOException {
        Files.writeString(INBOX.resolve("roto.pdf"), "esto no es un PDF");

        List<ImportRecord> records = scanner.scanNow();

        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.getProcessingStatus()).isEqualTo(ImportStatus.ERROR);
            assertThat(record.getErrorMessage()).contains("no se pudo leer como PDF");
            assertThat(record.getReceipt()).isNull();
        });
        assertThat(namesIn(INBOX)).isEmpty();
        assertThat(namesIn(FAILED)).containsExactly("roto.pdf");
        assertThat(receiptRepository.count()).isZero();
    }

    @Test
    @DisplayName("un ticket de otro comercio acaba en ERROR explicando que no se reconoce")
    void unknownMerchantEndsUpInError() throws IOException {
        Files.write(INBOX.resolve("carrefour.pdf"), TicketFixtures.unknownMerchantPdf());

        List<ImportRecord> records = scanner.scanNow();

        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.getProcessingStatus()).isEqualTo(ImportStatus.ERROR);
            assertThat(record.getErrorMessage()).contains("Mercadona");
        });
        assertThat(namesIn(FAILED)).containsExactly("carrefour.pdf");
    }

    @Test
    @DisplayName("lo que no es PDF se ignora y se queda donde está")
    void nonPdfFilesAreLeftAlone() throws IOException {
        Files.writeString(INBOX.resolve("notas.txt"), "esto no es un ticket");

        assertThat(scanner.scanNow()).isEmpty();
        assertThat(namesIn(INBOX)).containsExactly("notas.txt");
    }

    @Test
    @DisplayName("un fallo al guardar no borra el rastro del intento")
    void errorRecordSurvivesEvenIfNothingWasPersisted() throws IOException {
        Files.writeString(INBOX.resolve("roto.pdf"), "no es un PDF");

        scanner.scanNow();

        // El registro de importación va en su propia transacción justamente para esto.
        assertThat(importRecordRepository.count()).isEqualTo(1);
        assertThat(receiptRepository.count()).isZero();
    }

    // -------------------------------------------------------------------- subida manual

    @Test
    @DisplayName("la subida manual pasa por el mismo pipeline y guarda copia en procesados")
    void manualUploadUsesTheSamePipeline() throws IOException {
        ImportRecord record = importService.importUpload(
                TicketFixtures.pdfOf(TicketFixtures.TICKET_33_63), "subido-a-mano.pdf");

        assertThat(record.getProcessingStatus()).isEqualTo(ImportStatus.PROCESSED);
        assertThat(record.getSource()).isEqualTo(ImportSource.MERCADONA);
        assertThat(receiptRepository.count()).isEqualTo(1);
        assertThat(receiptRepository.findAll().getFirst().getSourceType())
                .isEqualTo(SourceType.UPLOAD);
        assertThat(namesIn(PROCESSED)).containsExactly("subido-a-mano.pdf");
    }

    @Test
    @DisplayName("subir a mano un ticket que ya entró por la carpeta es DUPLICATE")
    void manualUploadOfAnAlreadyImportedTicketIsDuplicate() {
        dropInInbox(TicketFixtures.TICKET_2_45);
        scanner.scanNow();

        ImportRecord record = importService.importUpload(
                TicketFixtures.pdfOf(TicketFixtures.TICKET_2_45), "otra-vez.pdf");

        assertThat(record.getProcessingStatus()).isEqualTo(ImportStatus.DUPLICATE);
        assertThat(receiptRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("una subida manual que falla deja copia en error/ y el motivo registrado")
    void failedManualUploadIsStoredInErrorFolder() throws IOException {
        ImportRecord record = importService.importUpload(
                "no soy un PDF".getBytes(), "basura.pdf");

        assertThat(record.getProcessingStatus()).isEqualTo(ImportStatus.ERROR);
        assertThat(record.getErrorMessage()).isNotBlank();
        assertThat(namesIn(FAILED)).containsExactly("basura.pdf");
    }

    // ------------------------------------------------------------------------ historial

    @Test
    @DisplayName("el historial ordena por fecha descendente y deja abrir el error")
    void historyIsOrderedAndErrorsCanBeOpened() throws IOException {
        dropInInbox(TicketFixtures.TICKET_3_77);
        Files.writeString(INBOX.resolve("zroto.pdf"), "no es un PDF");
        scanner.scanNow();

        var history = queryService.history(0, 20);

        assertThat(history.content()).hasSize(2);
        assertThat(history.totalElements()).isEqualTo(2);
        var error = history.content().stream()
                .filter(dto -> dto.status() == ImportStatus.ERROR)
                .findFirst()
                .orElseThrow();
        assertThat(error.totalAmount()).isNull();
        assertThat(queryService.detail(error.id()).errorMessage()).isNotBlank();

        var processed = history.content().stream()
                .filter(dto -> dto.status() == ImportStatus.PROCESSED)
                .findFirst()
                .orElseThrow();
        assertThat(processed.totalAmount()).isEqualByComparingTo(new BigDecimal("3.77"));
    }

    @Test
    @DisplayName("el estado informa de la carpeta, la última comprobación y los contadores")
    void statusReportsTheFolderAndCounters() {
        dropInInbox(TicketFixtures.TICKET_3_77);
        scanner.scanNow();

        var status = queryService.status();

        assertThat(status.folder().configured()).isTrue();
        assertThat(status.folder().path()).isEqualTo(INBOX.toString());
        assertThat(status.folder().exists()).isTrue();
        assertThat(status.folder().readable()).isTrue();
        assertThat(status.folder().pendingPdfs()).isZero();
        assertThat(status.lastCheckedAt()).isNotNull();
        assertThat(status.lastImportedAt()).isNotNull();
        assertThat(status.processedCount()).isEqualTo(1);
        assertThat(status.errorCount()).isZero();
    }

    @Test
    @DisplayName("cada importación guarda cuándo empezó y cuándo terminó")
    void importRecordsTrackStartAndFinish() {
        dropInInbox(TicketFixtures.TICKET_3_77);

        scanner.scanNow();

        assertThat(importRecordRepository.findAll()).singleElement().satisfies(record -> {
            assertThat(record.getStartedAt()).isNotNull();
            assertThat(record.getFinishedAt()).isNotNull();
            assertThat(record.getFinishedAt()).isAfterOrEqualTo(record.getStartedAt());
        });
    }

    @Test
    @DisplayName("dos pasadas a la vez no duplican nada: una de las dos se rechaza")
    void concurrentScansDoNotDuplicate() throws Exception {
        TicketFixtures.ALL.forEach(this::dropInInbox);

        // Las dos tareas salen a la vez desde el mismo latch. No se asume cuál gana el
        // cerrojo: lo que importa es que no se procese el mismo PDF dos veces.
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var go = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Object> scan = () -> {
            go.await();
            try {
                return scanner.scanNow();
            } catch (RuntimeException rejected) {
                return rejected;
            }
        };
        try {
            var first = executor.submit(scan);
            var second = executor.submit(scan);
            go.countDown();

            Object resultA = first.get(60, java.util.concurrent.TimeUnit.SECONDS);
            Object resultB = second.get(60, java.util.concurrent.TimeUnit.SECONDS);

            long rejections = java.util.stream.Stream.of(resultA, resultB)
                    .filter(result -> result instanceof RuntimeException)
                    .peek(result -> assertThat((RuntimeException) result)
                            .hasMessageContaining("en curso"))
                    .count();
            // Si coincidieron, una fue rechazada. Si no llegaron a coincidir, ninguna.
            assertThat(rejections).isBetween(0L, 1L);

            // La invariante que de verdad importa, gane quien gane el cerrojo.
            assertThat(receiptRepository.count()).isEqualTo(8);
            assertThat(importRecordRepository.findAll())
                    .filteredOn(record -> record.getProcessingStatus() == ImportStatus.PROCESSED)
                    .hasSize(8);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("el cerrojo queda libre cuando no hay ninguna pasada en marcha")
    void lockIsReleasedAfterScanning() {
        assertThat(scanner.isScanning()).isFalse();
        scanner.scanNow();
        assertThat(scanner.isScanning()).isFalse();
    }

    @Test
    @DisplayName("consultar una importación que no existe da 404")
    void unknownImportIsNotFound() {
        assertThatThrownBy(() -> queryService.detail(999_999L))
                .hasMessageContaining("No existe la importación");
    }

    // ------------------------------------------------------------------------ dashboard

    @Test
    @DisplayName("el dashboard sale de los tickets realmente importados")
    void dashboardComesFromTheImportedTickets() {
        TicketFixtures.ALL.forEach(this::dropInInbox);
        scanner.scanNow();

        var summary = analyticsService.summary(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(summary.receiptCount()).isEqualTo(8);
        assertThat(summary.totalSpent()).isEqualByComparingTo(new BigDecimal("202.70"));
        assertThat(summary.averageTicket()).isEqualByComparingTo(new BigDecimal("25.34"));

        var byPeriod = analyticsService.byPeriod(LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30), "day");
        // El 19/09 hubo dos compras: 19,00 + 48,75.
        assertThat(byPeriod).anySatisfy(point -> {
            assertThat(point.period()).isEqualTo("2026-09-19");
            assertThat(point.total()).isEqualByComparingTo(new BigDecimal("67.75"));
        });
    }

    @Test
    @DisplayName("el ticket del 26/09 conserva sus 30 líneas y el artículo al peso")
    void theBiggestTicketKeepsItsLinesAndWeightItem() {
        dropInInbox(TicketFixtures.TICKET_73_70);
        scanner.scanNow();

        // Por el servicio y no por el repositorio: las lineas son perezosas y aqui fuera
        // no hay sesion de Hibernate abierta (open-in-view esta desactivado).
        var detail = receiptService.detail(receiptRepository.findAll().getFirst().getId());

        assertThat(detail.totalAmount()).isEqualByComparingTo(new BigDecimal("73.70"));
        assertThat(detail.invoiceNumber()).isEqualTo("2345-012-184052");
        assertThat(detail.items()).hasSize(30);
        assertThat(detail.taxLines()).hasSize(3);
        assertThat(detail.items())
                .filteredOn(item -> item.rawDescription().equals("PIMIENTO ROJO"))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.weightKg()).isEqualByComparingTo(new BigDecimal("0.718"));
                    assertThat(item.pricePerKg()).isEqualByComparingTo(new BigDecimal("2.50"));
                });
    }
}
