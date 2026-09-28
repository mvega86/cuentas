package mvega.dev.cuentas.receipt.importer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import mvega.dev.cuentas.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Los endpoints de importación por HTTP, con el JSON que ve de verdad el frontend.
 *
 * <p>Existe porque faltaba: el pipeline estaba cubierto a nivel de servicio, pero nada
 * comprobaba la serialización de la respuesta. Un duplicado devolvía 500 porque el DTO
 * tocaba una colección perezosa fuera de la sesión de Hibernate, y ningún test lo veía.
 */
@AutoConfigureMockMvc
class ImportApiTest extends PostgresIntegrationTest {

    static final Path BASE = Path.of(System.getProperty("java.io.tmpdir"), "cuentas-api-test");
    static final Path INBOX = BASE.resolve("entrada");
    static final Path PROCESSED = BASE.resolve("procesados");
    static final Path FAILED = BASE.resolve("error");

    @DynamicPropertySource
    static void importProperties(DynamicPropertyRegistry registry) {
        registry.add("cuentas.imports.mercadona.directory", INBOX::toString);
        registry.add("cuentas.imports.mercadona.processed-directory", PROCESSED::toString);
        registry.add("cuentas.imports.mercadona.error-directory", FAILED::toString);
        registry.add("cuentas.imports.scan-delay-ms", () -> 3_600_000);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void reset() throws IOException {
        jdbcClient.sql("delete from import_record").update();
        jdbcClient.sql("truncate table receipt cascade").update();
        for (Path dir : List.of(INBOX, PROCESSED, FAILED)) {
            Files.createDirectories(dir);
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
    }

    private void dropInInbox(String fixture, String filename) throws IOException {
        Files.write(INBOX.resolve(filename), TicketFixtures.pdfOf(fixture));
    }

    @Test
    @DisplayName("POST scan devuelve 200 y el resumen de la pasada")
    void scanReturnsTheRunSummary() throws Exception {
        dropInInbox(TicketFixtures.TICKET_73_70, "ticket.pdf");

        mockMvc.perform(post("/api/imports/mercadona/scan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(1))
                .andExpect(jsonPath("$.processed").value(1))
                .andExpect(jsonPath("$.errors").value(0))
                .andExpect(jsonPath("$.results[0].status").value("PROCESSED"))
                .andExpect(jsonPath("$.results[0].totalAmount").value(73.70))
                .andExpect(jsonPath("$.results[0].receiptId", notNullValue()))
                .andExpect(jsonPath("$.results[0].startedAt", notNullValue()))
                .andExpect(jsonPath("$.results[0].finishedAt", notNullValue()));
    }

    @Test
    @DisplayName("un duplicado devuelve 200, no 500")
    void duplicateScanDoesNotBlowUp() throws Exception {
        byte[] pdf = TicketFixtures.pdfOf(TicketFixtures.TICKET_73_70);
        Files.write(INBOX.resolve("primera.pdf"), pdf);
        mockMvc.perform(post("/api/imports/mercadona/scan")).andExpect(status().isOk());

        Files.write(INBOX.resolve("segunda.pdf"), pdf);

        mockMvc.perform(post("/api/imports/mercadona/scan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicates").value(1))
                .andExpect(jsonPath("$.results[0].status").value("DUPLICATE"))
                .andExpect(jsonPath("$.results[0].errorMessage",
                        containsString("ya estaba importado")));
    }

    @Test
    @DisplayName("un fichero ilegible devuelve 200 con el registro en ERROR")
    void errorScanReturnsOkWithErrorRecord() throws Exception {
        Files.writeString(INBOX.resolve("roto.pdf"), "no es un PDF");

        mockMvc.perform(post("/api/imports/mercadona/scan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").value(1))
                .andExpect(jsonPath("$.results[0].status").value("ERROR"))
                .andExpect(jsonPath("$.results[0].errorMessage", notNullValue()))
                .andExpect(jsonPath("$.results[0].totalAmount").value(is(nullValue())));
    }

    @Test
    @DisplayName("un documento ajeno devuelve 200 con UNSUPPORTED y sin importe")
    void unsupportedDocumentIsReported() throws Exception {
        Files.write(INBOX.resolve("otro.pdf"), TicketFixtures.otherMerchantPdf());

        mockMvc.perform(post("/api/imports/mercadona/scan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unsupported").value(1))
                .andExpect(jsonPath("$.processed").value(0))
                .andExpect(jsonPath("$.errors").value(0))
                .andExpect(jsonPath("$.results[0].status").value("UNSUPPORTED"))
                .andExpect(jsonPath("$.results[0].errorMessage")
                        .value("Documento no reconocido como ticket de Mercadona"))
                .andExpect(jsonPath("$.results[0].receiptId").value(is(nullValue())))
                .andExpect(jsonPath("$.results[0].totalAmount").value(is(nullValue())));

        // No se ha creado ningún ticket.
        mockMvc.perform(get("/api/receipts"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("subir a mano un documento ajeno también es UNSUPPORTED")
    void unsupportedManualUploadIsReported() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "ajeno.pdf",
                MediaType.APPLICATION_PDF_VALUE, TicketFixtures.otherMerchantPdf());

        mockMvc.perform(multipart("/api/imports/mercadona/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unsupported").value(1))
                .andExpect(jsonPath("$.results[0].status").value("UNSUPPORTED"));
    }

    @Test
    @DisplayName("POST upload acepta multipart y devuelve el resumen")
    void uploadAcceptsMultipart() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "a-mano.pdf",
                MediaType.APPLICATION_PDF_VALUE, TicketFixtures.pdfOf(TicketFixtures.TICKET_2_45));

        mockMvc.perform(multipart("/api/imports/mercadona/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1))
                .andExpect(jsonPath("$.results[0].originalFilename").value("a-mano.pdf"))
                .andExpect(jsonPath("$.results[0].totalAmount").value(2.45));
    }

    @Test
    @DisplayName("GET status describe la carpeta configurada")
    void statusDescribesTheFolder() throws Exception {
        mockMvc.perform(get("/api/imports/mercadona/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folder.configured").value(true))
                .andExpect(jsonPath("$.folder.path").value(INBOX.toString()))
                .andExpect(jsonPath("$.folder.exists").value(true))
                .andExpect(jsonPath("$.processedCount").value(0));
    }

    @Test
    @DisplayName("GET historial y detalle devuelven el número de líneas del ticket")
    void historyAndDetailAreServed() throws Exception {
        dropInInbox(TicketFixtures.TICKET_73_70, "ticket.pdf");
        mockMvc.perform(post("/api/imports/mercadona/scan")).andExpect(status().isOk());

        mockMvc.perform(get("/api/imports").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PROCESSED"));

        String body = mockMvc.perform(get("/api/imports").param("size", "10"))
                .andReturn().getResponse().getContentAsString();
        long id = com.jayway.jsonpath.JsonPath.parse(body).read("$.content[0].id", Long.class);

        // El detalle sí corre dentro de una transacción, así que puede contar las líneas.
        mockMvc.perform(get("/api/imports/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(30))
                .andExpect(jsonPath("$.sha256", notNullValue()));
    }

    @Test
    @DisplayName("pedir una importación inexistente devuelve 404 con la estructura de error")
    void unknownImportReturns404() throws Exception {
        mockMvc.perform(get("/api/imports/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("No existe la importación")))
                .andExpect(jsonPath("$.path").value("/api/imports/999999"));
    }

    @Test
    @DisplayName("GET /api/receipts devuelve la página de tickets importados")
    void receiptsAreServed() throws Exception {
        dropInInbox(TicketFixtures.TICKET_33_63, "ticket.pdf");
        mockMvc.perform(post("/api/imports/mercadona/scan")).andExpect(status().isOk());

        mockMvc.perform(get("/api/receipts").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].totalAmount").value(33.63))
                .andExpect(jsonPath("$.content[0].itemCount").value(12))
                .andExpect(jsonPath("$.content[0].sourceType").value("FOLDER"))
                .andExpect(jsonPath("$.totalPages").value(greaterThanOrEqualTo(1)));
    }

    private static org.hamcrest.Matcher<Object> nullValue() {
        return org.hamcrest.Matchers.nullValue();
    }
}
