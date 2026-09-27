package mvega.dev.cuentas.receipt.importer;

import java.math.BigDecimal;

/** Una fila del desglose de IVA leida del ticket. */
public record ParsedTaxLine(
        BigDecimal ratePercent,
        BigDecimal taxableBase,
        BigDecimal taxAmount,
        BigDecimal totalAmount) {}
