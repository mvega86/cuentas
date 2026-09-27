package mvega.dev.cuentas.receipt.dto;

import jakarta.validation.constraints.Size;

/**
 * Cambio manual sobre una linea (CLAUDE §10, pantalla 3).
 *
 * <p>Con {@code saveAsRule} a true, la categoria elegida se guarda tambien como regla
 * para que las compras futuras del mismo producto la hereden (CLAUDE §5).
 */
public record UpdateReceiptItemRequest(
        @Size(max = 240) String normalizedName,
        Long categoryId,
        boolean saveAsRule) {}
