package mvega.dev.cuentas.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Un precio unitario observado en una fecha, para dibujar la evolucion (CLAUDE §11). */
public record PricePointDto(LocalDateTime purchasedAt, BigDecimal unitPrice) {}
