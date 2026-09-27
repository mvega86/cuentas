package mvega.dev.cuentas.receipt.importer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import mvega.dev.cuentas.config.CuentasProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Las tres carpetas del pipeline de importación: entrada, procesados y error.
 *
 * <p>Las tres se configuran por separado, porque el montaje puede estar organizado de
 * cualquier manera:
 *
 * <pre>
 * CUENTAS_IMPORT_MERCADONA_DIR             ← la llena la sincronización externa
 * CUENTAS_IMPORT_MERCADONA_PROCESSED_DIR   ← aquí acaba lo que se importó bien o ya estaba
 * CUENTAS_IMPORT_MERCADONA_ERROR_DIR       ← aquí acaba lo que no se pudo procesar
 * </pre>
 *
 * <p>Cuentas no sabe de dónde vienen esas carpetas: rclone, Drive, un NAS o un directorio
 * local. No hay ninguna lógica acoplada a Google Drive ni a rclone; solo rutas del sistema
 * de ficheros. La aplicación funciona igual contra una carpeta local normal.
 */
@Component
public class ImportFolders {

    private static final Logger log = LoggerFactory.getLogger(ImportFolders.class);

    private final String configuredInbox;
    private final String configuredProcessed;
    private final String configuredFailed;

    ImportFolders(CuentasProperties properties) {
        CuentasProperties.Mercadona mercadona = properties.imports() == null
                ? null
                : properties.imports().mercadona();
        this.configuredInbox = mercadona == null ? null : mercadona.directory();
        this.configuredProcessed = mercadona == null ? null : mercadona.processedDirectory();
        this.configuredFailed = mercadona == null ? null : mercadona.errorDirectory();
    }

    /** Ruta configurada tal cual, sin validar. */
    public String configuredPath() {
        return configuredInbox == null || configuredInbox.isBlank() ? null : configuredInbox.trim();
    }

    /**
     * Comprueba el estado de la carpeta de entrada sin lanzar excepciones: la interfaz
     * necesita poder explicar qué falta.
     */
    public FolderState state() {
        String configured = configuredPath();
        if (configured == null) {
            return new FolderState(false, null, false, false, 0,
                    "Sin configurar. Define CUENTAS_IMPORT_MERCADONA_DIR con la ruta de la "
                            + "carpeta de entrada.");
        }

        Path inbox;
        try {
            inbox = inbox();
        } catch (InvalidPathException e) {
            return new FolderState(true, configured, false, false, 0,
                    "La ruta configurada no es válida: " + e.getMessage());
        }

        if (!Files.exists(inbox)) {
            return new FolderState(true, inbox.toString(), false, false, 0,
                    "La carpeta no existe todavía. Comprueba que la sincronización externa "
                            + "está funcionando y que la ruta es correcta.");
        }
        if (!Files.isDirectory(inbox)) {
            return new FolderState(true, inbox.toString(), true, false, 0,
                    "La ruta configurada existe pero no es una carpeta.");
        }
        if (!Files.isReadable(inbox)) {
            return new FolderState(true, inbox.toString(), true, false, 0,
                    "No hay permiso de lectura sobre la carpeta.");
        }
        try {
            return new FolderState(true, inbox.toString(), true, true, pendingPdfs().size(), null);
        } catch (IOException e) {
            return new FolderState(true, inbox.toString(), true, false, 0,
                    "No se pudo leer el contenido de la carpeta: " + e.getMessage());
        }
    }

    public Path inbox() {
        String configured = configuredPath();
        if (configured == null) {
            throw new IllegalStateException("La carpeta de entrada no está configurada");
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }

    public Path processed() {
        return resolveOrSibling(configuredProcessed, "procesados");
    }

    public Path failed() {
        return resolveOrSibling(configuredFailed, "error");
    }

    /**
     * Usa la ruta configurada. Si no hay ninguna, cae en una hermana de la de entrada, para
     * que el pipeline siga funcionando aunque solo se configure la carpeta de entrada.
     */
    private Path resolveOrSibling(String configured, String fallbackName) {
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        }
        Path inbox = inbox();
        Path parent = inbox.getParent();
        return parent == null ? inbox.resolve(fallbackName) : parent.resolve(fallbackName);
    }

    /** PDFs del primer nivel de la carpeta de entrada, en orden estable por nombre. */
    public List<Path> pendingPdfs() throws IOException {
        try (Stream<Path> entries = Files.list(inbox())) {
            return entries
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)
                            .endsWith(".pdf"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    /**
     * Mueve un fichero a la carpeta de procesados.
     *
     * @return dónde quedó, o {@code null} si no se pudo mover
     */
    public Path moveToProcessed(Path file) {
        return moveTo(file, processed());
    }

    /**
     * Mueve un fichero a la carpeta de error.
     *
     * @return dónde quedó, o {@code null} si no se pudo mover
     */
    public Path moveToFailed(Path file) {
        return moveTo(file, failed());
    }

    /** Guarda unos bytes en una carpeta del pipeline, para las subidas manuales. */
    public Path store(byte[] content, String filename, Path targetDir) {
        try {
            Files.createDirectories(targetDir);
            Path target = freeName(targetDir, filename);
            Files.write(target, content);
            return target;
        } catch (IOException e) {
            log.warn("No se pudo guardar {} en {}", filename, targetDir, e);
            return null;
        }
    }

    private Path moveTo(Path file, Path targetDir) {
        if (file == null) {
            return null;
        }
        try {
            Files.createDirectories(targetDir);
            Path target = freeName(targetDir, file.getFileName().toString());
            Files.move(file, target, StandardCopyOption.ATOMIC_MOVE);
            return target;
        } catch (IOException atomicFailed) {
            // Entre sistemas de ficheros distintos el movimiento atómico no se puede, por
            // ejemplo si la entrada es un montaje de rclone y el destino es disco local.
            try {
                Path target = freeName(targetDir, file.getFileName().toString());
                Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                return target;
            } catch (IOException e) {
                log.warn("No se pudo mover {} a {}", file, targetDir, e);
                return null;
            }
        }
    }

    /** Evita pisar un fichero que ya exista con el mismo nombre. */
    private Path freeName(Path dir, String filename) {
        Path candidate = dir.resolve(filename);
        if (!Files.exists(candidate)) {
            return candidate;
        }
        int dot = filename.lastIndexOf('.');
        String base = dot > 0 ? filename.substring(0, dot) : filename;
        String extension = dot > 0 ? filename.substring(dot) : "";
        for (int n = 2; n < 1000; n++) {
            candidate = dir.resolve("%s (%d)%s".formatted(base, n, extension));
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
        return dir.resolve("%s (%d)%s".formatted(base, System.currentTimeMillis(), extension));
    }

    /**
     * Estado de la carpeta de entrada.
     *
     * @param configured si hay ruta configurada
     * @param path la ruta resuelta, o {@code null} si no hay
     * @param exists si esa ruta existe
     * @param readable si se puede listar
     * @param pendingPdfs cuántos PDFs esperan en el primer nivel
     * @param message explicación cuando algo no está bien, {@code null} si todo va
     */
    public record FolderState(
            boolean configured,
            String path,
            boolean exists,
            boolean readable,
            int pendingPdfs,
            String message) {}
}
