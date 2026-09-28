package mvega.dev.cuentas.receipt.importer.dto;

import java.util.List;
import mvega.dev.cuentas.receipt.importer.ImportRecord;
import mvega.dev.cuentas.receipt.importer.ImportStatus;

/**
 * Resultado de una pasada de importación: cuántos ficheros se vieron y en qué acabó cada
 * uno (CLAUDE §7 paso 11).
 */
public record ImportRunDto(
        int found,
        int processed,
        int duplicates,
        int unsupported,
        int errors,
        int needingReview,
        List<ImportRecordDto> results) {

    public static ImportRunDto from(List<ImportRecord> records) {
        int processed = 0;
        int duplicates = 0;
        int unsupported = 0;
        int errors = 0;
        int review = 0;
        // summaryFrom y no from: al terminar una pasada los tickets ya están desligados de
        // la sesión de Hibernate, y contar sus líneas intentaría cargar una colección
        // perezosa sin sesión. El número de líneas se consulta en el detalle, que sí corre
        // dentro de una transacción.
        List<ImportRecordDto> results =
                records.stream().map(ImportRecordDto::summaryFrom).toList();
        for (ImportRecord record : records) {
            switch (record.getProcessingStatus()) {
                case PROCESSED -> {
                    processed++;
                    if (record.getReceipt() != null && record.getReceipt()
                            .getParsingStatus() == mvega.dev.cuentas.receipt.ParsingStatus.REVIEW) {
                        review++;
                    }
                }
                case DUPLICATE -> duplicates++;
                case UNSUPPORTED -> unsupported++;
                case ERROR -> errors++;
                case PENDING, PROCESSING -> { /* no deberia quedar ninguno al terminar */ }
            }
        }
        return new ImportRunDto(records.size(), processed, duplicates, unsupported, errors,
                review, results);
    }

    /** Pasada sin nada que hacer, para cuando la carpeta está vacía. */
    public static ImportRunDto empty() {
        return new ImportRunDto(0, 0, 0, 0, 0, 0, List.of());
    }

    public static ImportRunDto ofSingle(ImportRecord record) {
        return from(List.of(record));
    }

    /** Si la pasada dejó algo sin reconocer, con error o en revisión, conviene avisar. */
    public boolean needsAttention() {
        return errors > 0 || unsupported > 0 || needingReview > 0;
    }

    public static boolean isFinal(ImportStatus status) {
        return status == ImportStatus.PROCESSED || status == ImportStatus.DUPLICATE
                || status == ImportStatus.UNSUPPORTED || status == ImportStatus.ERROR;
    }
}
