package mvega.dev.cuentas.receipt.importer;

import java.time.Instant;
import mvega.dev.cuentas.receipt.importer.dto.ImportRecordDto;
import mvega.dev.cuentas.receipt.importer.dto.ImportStatusDto;
import mvega.dev.cuentas.shared.error.NotFoundException;
import mvega.dev.cuentas.shared.web.PageDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas sobre el historial de importaciones. */
@Service
public class ImportQueryService {

    private final ImportRecordRepository repository;
    private final ImportFolders folders;
    private final MercadonaFolderScanner scanner;

    ImportQueryService(ImportRecordRepository repository, ImportFolders folders,
            MercadonaFolderScanner scanner) {
        this.repository = repository;
        this.folders = folders;
        this.scanner = scanner;
    }

    @Transactional(readOnly = true)
    public PageDto<ImportRecordDto> history(int page, int size) {
        return PageDto.from(repository.findHistory(PageRequest.of(page, Math.min(size, 200)))
                .map(ImportRecordDto::summaryFrom));
    }

    @Transactional(readOnly = true)
    public ImportRecordDto detail(Long id) {
        return repository.findDetailById(id)
                .map(ImportRecordDto::from)
                .orElseThrow(() -> new NotFoundException("No existe la importación " + id));
    }

    @Transactional(readOnly = true)
    public ImportStatusDto status() {
        Instant lastImported = repository
                .findFirstByProcessingStatusOrderByFinishedAtDesc(ImportStatus.PROCESSED)
                .map(ImportRecord::getFinishedAt)
                .orElse(null);
        return new ImportStatusDto(
                folders.state(),
                scanner.lastCheckedAt(),
                lastImported,
                repository.countByProcessingStatus(ImportStatus.PROCESSED),
                repository.countByProcessingStatus(ImportStatus.ERROR));
    }
}
