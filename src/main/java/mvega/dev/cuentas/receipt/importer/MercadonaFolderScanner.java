package mvega.dev.cuentas.receipt.importer;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import mvega.dev.cuentas.shared.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Revisa periódicamente la carpeta de entrada.
 *
 * <p>Para la primera versión se prioriza simplicidad y fiabilidad: un sondeo con
 * {@code fixedDelay} basta y evita la complejidad de los eventos de sistema de ficheros. La
 * frecuencia es configurable con {@code CUENTAS_IMPORT_SCAN_DELAY_MS}.
 *
 * <p>{@code fixedDelay} y no {@code fixedRate} a propósito: la espera cuenta desde que
 * <em>termina</em> la pasada anterior, así dos pasadas programadas no se solapan aunque una
 * tarde más de lo previsto.
 *
 * <p>Eso no basta, porque el botón de la interfaz puede pedir una pasada mientras el sondeo
 * está trabajando. Un cerrojo garantiza que solo haya una pasada a la vez y que un mismo PDF
 * no se procese dos veces en paralelo.
 */
@Component
public class MercadonaFolderScanner {

    private static final Logger log = LoggerFactory.getLogger(MercadonaFolderScanner.class);

    private final MercadonaImportService importService;
    private final AtomicReference<Instant> lastCheckedAt = new AtomicReference<>();
    private final ReentrantLock scanLock = new ReentrantLock();

    MercadonaFolderScanner(MercadonaImportService importService) {
        this.importService = importService;
    }

    /** Cuándo se revisó la carpeta por última vez, con resultado o sin él. */
    public Instant lastCheckedAt() {
        return lastCheckedAt.get();
    }

    /** Si hay una pasada en curso ahora mismo. */
    public boolean isScanning() {
        return scanLock.isLocked();
    }

    @Scheduled(fixedDelayString = "${cuentas.imports.scan-delay-ms:30000}",
            initialDelayString = "${cuentas.imports.scan-delay-ms:30000}")
    public void scanPeriodically() {
        if (!scanLock.tryLock()) {
            // Alguien pidió una pasada a mano y sigue en marcha. No se hace nada: la
            // siguiente vuelta del planificador recogerá lo que quede.
            log.debug("Pasada automática omitida: ya hay una en curso");
            return;
        }
        try {
            List<ImportRecord> records = doScan();
            if (!records.isEmpty()) {
                log.info("Pasada automática: {} ficheros procesados", records.size());
            }
        } catch (RuntimeException e) {
            // Una carpeta sin configurar o sin montar no debe llenar el log de trazas ni
            // matar el planificador: se avisa y se sigue.
            log.warn("Pasada automática no realizada: {}", e.getMessage());
        } finally {
            scanLock.unlock();
        }
    }

    /**
     * Pasada inmediata, la que dispara el botón de la interfaz y usan los tests.
     *
     * @throws ApiException si ya hay una pasada en curso
     */
    public List<ImportRecord> scanNow() {
        if (!scanLock.tryLock()) {
            throw new ApiException(
                    "Ya hay una revisión de la carpeta en curso. Espera a que termine.",
                    HttpStatus.CONFLICT);
        }
        try {
            return doScan();
        } finally {
            scanLock.unlock();
        }
    }

    private List<ImportRecord> doScan() {
        lastCheckedAt.set(Instant.now());
        return importService.scanInbox();
    }
}
