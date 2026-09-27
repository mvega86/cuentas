package mvega.dev.cuentas.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resumen de gasto de un periodo y su comparacion con el periodo anterior de la misma
 * duracion (CLAUDE §10, §11).
 */
public record SpendSummaryDto(
        LocalDate from,
        LocalDate to,
        BigDecimal totalSpent,
        long receiptCount,
        BigDecimal averageTicket,
        BigDecimal previousTotalSpent,
        BigDecimal changePercent) {}
