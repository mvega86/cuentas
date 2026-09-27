package mvega.dev.cuentas.shared.web;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Pagina con forma propia y estable.
 *
 * <p>Se devuelve esto en lugar de un {@code Page} de Spring Data porque su JSON no es
 * un contrato publico y cambia entre versiones. Asi el frontend tiene una forma fija.
 */
public record PageDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageDto<T> from(Page<T> page) {
        return new PageDto<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
