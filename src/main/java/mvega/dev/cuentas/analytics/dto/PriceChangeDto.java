package mvega.dev.cuentas.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Producto cuyo precio ha cambiado entre las dos ultimas compras (CLAUDE §10). */
public record PriceChangeDto(
        String normalizedName,
        BigDecimal currentPrice,
        BigDecimal previousPrice,
        BigDecimal changePercent,
        LocalDateTime lastSeenAt) {}
