package mvega.dev.cuentas.receipt.importer;

import java.nio.file.Path;
import java.time.Instant;
import mvega.dev.cuentas.receipt.Receipt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escribe el registro de importaciones.
 *
 * <p>Cada método va en su propia transacción ({@code REQUIRES_NEW}) a propósito: si el
 * guardado del ticket se deshace, el rastro del intento y el motivo del fallo tienen que
 * sobrevivir. Si compartieran transacción, un error borraría la única prueba de que pasó.
 */
@Service
public class ImportRecordService {

    private final ImportRecordRepository repository;

    ImportRecordService(ImportRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportRecord start(ImportSource source, String filename, String sha256) {
        return repository.save(ImportRecord.builder()
                .source(source)
                .originalFilename(filename)
                .sha256(sha256)
                .processingStatus(ImportStatus.PENDING)
                .startedAt(Instant.now())
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessing(Long id) {
        repository.findById(id)
                .ifPresent(record -> record.setProcessingStatus(ImportStatus.PROCESSING));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportRecord markProcessed(Long id, Receipt receipt, Path storedPath) {
        return finish(id, ImportStatus.PROCESSED, receipt, null, storedPath);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportRecord markDuplicate(Long id, Receipt existing, String message, Path storedPath) {
        return finish(id, ImportStatus.DUPLICATE, existing, message, storedPath);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportRecord markError(Long id, String message, Path storedPath) {
        return finish(id, ImportStatus.ERROR, null, message, storedPath);
    }

    private ImportRecord finish(Long id, ImportStatus status, Receipt receipt, String message,
            Path storedPath) {
        ImportRecord record = repository.findById(id).orElseThrow(
                () -> new IllegalStateException("No existe el registro de importación " + id));
        record.setProcessingStatus(status);
        record.setReceipt(receipt);
        record.setErrorMessage(message);
        record.setStoredPath(storedPath == null ? null : storedPath.toString());
        record.setFinishedAt(Instant.now());
        return record;
    }
}
