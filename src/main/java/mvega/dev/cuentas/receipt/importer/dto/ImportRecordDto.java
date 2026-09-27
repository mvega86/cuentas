package mvega.dev.cuentas.receipt.importer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import mvega.dev.cuentas.receipt.importer.ImportRecord;
import mvega.dev.cuentas.receipt.importer.ImportSource;
import mvega.dev.cuentas.receipt.importer.ImportStatus;

/**
 * Una fila del historial de importaciones (CLAUDE §9).
 *
 * <p>Lleva el importe y la fecha del ticket cuando hubo ticket; en un DUPLICATE o un ERROR
 * van a null y la interfaz muestra un guion. {@code errorMessage} es lo que se abre para
 * consultar por qué falló.
 */
public record ImportRecordDto(
        Long id,
        ImportSource source,
        String originalFilename,
        ImportStatus status,
        String errorMessage,
        UUID receiptId,
        BigDecimal totalAmount,
        LocalDateTime purchasedAt,
        Integer itemCount,
        String sha256,
        String storedPath,
        Instant startedAt,
        Instant finishedAt) {

    public static ImportRecordDto from(ImportRecord record) {
        var receipt = record.getReceipt();
        return new ImportRecordDto(
                record.getId(),
                record.getSource(),
                record.getOriginalFilename(),
                record.getProcessingStatus(),
                record.getErrorMessage(),
                receipt == null ? null : receipt.getId(),
                receipt == null ? null : receipt.getTotalAmount(),
                receipt == null ? null : receipt.getPurchasedAt(),
                receipt == null ? null : receipt.getItems().size(),
                record.getSha256(),
                record.getStoredPath(),
                record.getStartedAt(),
                record.getFinishedAt());
    }

    /** Versión ligera para listados: no toca las líneas del ticket, así evita un N+1. */
    public static ImportRecordDto summaryFrom(ImportRecord record) {
        var receipt = record.getReceipt();
        return new ImportRecordDto(
                record.getId(),
                record.getSource(),
                record.getOriginalFilename(),
                record.getProcessingStatus(),
                record.getErrorMessage(),
                receipt == null ? null : receipt.getId(),
                receipt == null ? null : receipt.getTotalAmount(),
                receipt == null ? null : receipt.getPurchasedAt(),
                null,
                record.getSha256(),
                record.getStoredPath(),
                record.getStartedAt(),
                record.getFinishedAt());
    }
}
