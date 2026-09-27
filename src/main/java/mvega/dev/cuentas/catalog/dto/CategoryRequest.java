package mvega.dev.cuentas.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Validacion en la entrada, que antes no existia en ningun DTO (CLAUDE §2, §9). */
public record CategoryRequest(@NotBlank @Size(max = 80) String name) {}
