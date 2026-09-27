package mvega.dev.cuentas.receipt.importer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import mvega.dev.cuentas.receipt.ParsingStatus;

/**
 * Resultado de parsear un ticket, independiente de JPA y de la fuente del PDF.
 *
 * <p>{@code warnings} recoge lo que el parser no supo interpretar. Una linea que no
 * se entiende no tumba el ticket entero: se guarda con estado {@code REVIEW} para
 * que el usuario lo repase (CLAUDE §6).
 */
public record ParsedReceipt(
        String merchantName,
        String taxId,
        String storeName,
        String storeAddress,
        String storePhone,
        LocalDateTime purchasedAt,
        String operationNumber,
        String invoiceNumber,
        String currency,
        BigDecimal totalAmount,
        String paymentMethod,
        String cardLast4,
        List<ParsedReceiptItem> items,
        List<ParsedTaxLine> taxLines,
        List<String> warnings,
        ParsingStatus status) {}
