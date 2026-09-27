package mvega.dev.cuentas.catalog.dto;

public record ProductRuleDto(
        Long id,
        String matchText,
        String normalizedName,
        Long categoryId,
        String categoryName,
        boolean active) {}
