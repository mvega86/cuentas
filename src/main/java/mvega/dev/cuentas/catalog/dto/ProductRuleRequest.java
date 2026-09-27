package mvega.dev.cuentas.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRuleRequest(
        @NotBlank @Size(max = 240) String matchText,
        @NotNull Long categoryId,
        Boolean active) {}
