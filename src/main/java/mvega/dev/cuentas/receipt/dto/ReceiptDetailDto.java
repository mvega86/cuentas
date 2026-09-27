package mvega.dev.cuentas.receipt.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import mvega.dev.cuentas.receipt.ParsingStatus;
import mvega.dev.cuentas.receipt.SourceType;

/** Detalle completo de un ticket (CLAUDE §10, pantalla 3). */
public record ReceiptDetailDto(
        UUID id,
        LocalDateTime purchasedAt,
        String merchantName,
        String storeAddress,
        String storePhone,
        String operationNumber,
        String invoiceNumber,
        String currency,
        BigDecimal totalAmount,
        String paymentMethod,
        String cardLast4,
        SourceType sourceType,
        String sourceFilename,
        ParsingStatus parsingStatus,
        String parserVersion,
        String parserWarnings,
        Instant importedAt,
        List<ReceiptItemDto> items,
        List<ReceiptTaxLineDto> taxLines) {}
