package mvega.dev.cuentas.analytics.dto;

import java.math.BigDecimal;

public record CategorySpendDto(Long categoryId, String categoryName, BigDecimal total) {}
