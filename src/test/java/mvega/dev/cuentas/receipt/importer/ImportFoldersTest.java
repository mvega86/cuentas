package mvega.dev.cuentas.receipt.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import mvega.dev.cuentas.config.CuentasProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Resolución de las tres carpetas y casos en los que la entrada no sirve. Sin base de datos. */
class ImportFoldersTest {

    private ImportFolders foldersFor(String directory) {
        return foldersFor(directory, null, null);
    }

    private ImportFolders foldersFor(String directory, String processed, String failed) {
        return new ImportFolders(new CuentasProperties(
                new CuentasProperties.Cors(List.of("http://localhost:4201")),
                new CuentasProperties.Imports(
                        new CuentasProperties.Mercadona(directory, processed, failed), 30_000)));
    }

    @Test
    @DisplayName("cada carpeta se puede configurar por separado")
    void everyFolderCanBeConfiguredOnItsOwn(@TempDir Path tempDir) {
        Path inbox = tempDir.resolve("Entrada");
        Path done = tempDir.resolve("otro/sitio/Procesados");
        Path bad = tempDir.resolve("mas/alla/Error");

        ImportFolders folders = foldersFor(inbox.toString(), done.toString(), bad.toString());

        assertThat(folders.inbox()).isEqualTo(inbox.toAbsolutePath().normalize());
        assertThat(folders.processed()).isEqualTo(done.toAbsolutePath().normalize());
        assertThat(folders.failed()).isEqualTo(bad.toAbsolutePath().normalize());
    }

    @Test
    @DisplayName("sin configurar procesados ni error, caen en hermanas de la entrada")
    void siblingFoldersAreDerivedFromTheInbox(@TempDir Path tempDir) {
        Path inbox = tempDir.resolve("entrada");
        ImportFolders folders = foldersFor(inbox.toString());

        assertThat(folders.inbox()).isEqualTo(inbox.toAbsolutePath().normalize());
        assertThat(folders.processed()).isEqualTo(tempDir.resolve("procesados").toAbsolutePath());
        assertThat(folders.failed()).isEqualTo(tempDir.resolve("error").toAbsolutePath());
    }

    @Test
    @DisplayName("sin configurar, lo dice en el estado en lugar de reventar")
    void missingConfigurationIsReported() {
        var state = foldersFor(null).state();

        assertThat(state.configured()).isFalse();
        assertThat(state.message()).contains("CUENTAS_IMPORT_MERCADONA_DIR");
    }

    @Test
    @DisplayName("una ruta en blanco cuenta como no configurada")
    void blankPathCountsAsNotConfigured() {
        assertThat(foldersFor("   ").state().configured()).isFalse();
    }

    @Test
    @DisplayName("si la carpeta no existe todavía, lo explica")
    void missingFolderIsExplained(@TempDir Path tempDir) {
        var state = foldersFor(tempDir.resolve("aun-no").toString()).state();

        assertThat(state.configured()).isTrue();
        assertThat(state.exists()).isFalse();
        assertThat(state.message()).contains("no existe todavía");
    }

    @Test
    @DisplayName("si la ruta es un fichero y no una carpeta, lo dice")
    void fileInsteadOfFolderIsExplained(@TempDir Path tempDir) throws Exception {
        Path file = Files.writeString(tempDir.resolve("no-soy-carpeta.txt"), "hola");

        var state = foldersFor(file.toString()).state();

        assertThat(state.exists()).isTrue();
        assertThat(state.readable()).isFalse();
        assertThat(state.message()).contains("no es una carpeta");
    }

    @Test
    @DisplayName("cuenta solo los PDFs del primer nivel, ignorando el resto")
    void countsOnlyTopLevelPdfs(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("uno.pdf"), "x");
        Files.writeString(tempDir.resolve("DOS.PDF"), "x");
        Files.writeString(tempDir.resolve("notas.txt"), "x");
        Files.createDirectory(tempDir.resolve("sub"));
        Files.writeString(tempDir.resolve("sub/tres.pdf"), "x");

        assertThat(foldersFor(tempDir.toString()).state().pendingPdfs()).isEqualTo(2);
    }

    @Test
    @DisplayName("mover dos ficheros con el mismo nombre no pisa el primero")
    void movingTwoFilesWithTheSameNameKeepsBoth(@TempDir Path tempDir) throws Exception {
        Path inbox = Files.createDirectories(tempDir.resolve("entrada"));
        ImportFolders folders = foldersFor(inbox.toString());

        Path first = Files.writeString(inbox.resolve("ticket.pdf"), "primero");
        folders.moveToProcessed(first);
        Path second = Files.writeString(inbox.resolve("ticket.pdf"), "segundo");
        folders.moveToProcessed(second);

        try (var entries = Files.list(folders.processed())) {
            assertThat(entries.map(p -> p.getFileName().toString()).sorted().toList())
                    .containsExactly("ticket (2).pdf", "ticket.pdf");
        }
    }
}
