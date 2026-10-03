package mvega.dev.cuentas.analytics.dto;

import java.math.BigDecimal;

public record AverageSpendDto(
        BigDecimal weeklyAverage,
        long completeWeeks,
        BigDecimal monthlyAverage,
        long completeMonths) {
}