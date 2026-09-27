package mvega.dev.cuentas.analytics.dto;

import java.math.BigDecimal;

/** Gasto agrupado por dia, semana o mes. {@code period} va en ISO (yyyy-MM-dd). */
public record PeriodSpendDto(String period, BigDecimal total) {}
