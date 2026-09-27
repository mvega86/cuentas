package mvega.dev.cuentas.analytics.dto;

import java.math.BigDecimal;

public record ProductSpendDto(
        String normalizedName,
        BigDecimal total,
        BigDecimal totalQuantity,
        long timesBought) {}
