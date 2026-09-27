package mvega.dev.cuentas.receipt.dto;

import java.math.BigDecimal;

public record ReceiptTaxLineDto(
        BigDecimal ratePercent,
        BigDecimal taxableBase,
        BigDecimal taxAmount,
        BigDecimal totalAmount) {}
