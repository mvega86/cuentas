package mvega.dev.cuentas.receipt.dto;

import java.math.BigDecimal;
import java.util.UUID;
import mvega.dev.cuentas.receipt.CategorySource;
import mvega.dev.cuentas.receipt.ItemUnit;

/** Linea de ticket expuesta por la API. Nunca se devuelve la entidad JPA (CLAUDE §9). */
public record ReceiptItemDto(
        UUID id,
        Integer lineNumber,
        String rawDescription,
        String normalizedName,
        BigDecimal quantity,
        ItemUnit unit,
        BigDecimal unitPrice,
        BigDecimal weightKg,
        BigDecimal pricePerKg,
        BigDecimal lineTotal,
        Long categoryId,
        String categoryName,
        CategorySource categorySource) {}
