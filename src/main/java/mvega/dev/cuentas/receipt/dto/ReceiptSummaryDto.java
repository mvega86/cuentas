package mvega.dev.cuentas.receipt.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import mvega.dev.cuentas.receipt.ParsingStatus;
import mvega.dev.cuentas.receipt.SourceType;

/** Fila del listado de tickets (CLAUDE §10, pantalla 2). */
public record ReceiptSummaryDto(
        UUID id,
        LocalDateTime purchasedAt,
        String merchantName,
        BigDecimal totalAmount,
        Long itemCount,
        ParsingStatus parsingStatus,
        SourceType sourceType) {}
