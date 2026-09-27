package mvega.dev.cuentas.receipt;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import mvega.dev.cuentas.receipt.dto.ReceiptDetailDto;
import mvega.dev.cuentas.receipt.dto.ReceiptItemDto;
import mvega.dev.cuentas.receipt.dto.ReceiptSummaryDto;
import mvega.dev.cuentas.receipt.dto.UpdateReceiptItemRequest;
import mvega.dev.cuentas.shared.web.PageDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de tickets (CLAUDE §9). Rutas REST de verdad: sin {@code /adicionar} ni
 * {@code @CrossOrigin} por controlador, que ahora esta centralizado.
 */
@RestController
@RequestMapping("/api")
public class ReceiptController {

    private final ReceiptService receiptService;

    ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    /** Listado paginado con filtro de fechas, ambas inclusive. */
    @GetMapping("/receipts")
    public PageDto<ReceiptSummaryDto> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageDto.from(receiptService.list(
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay(),
                page, Math.min(size, 200)));
    }

    @GetMapping("/receipts/{id}")
    public ReceiptDetailDto detail(@PathVariable UUID id) {
        return receiptService.detail(id);
    }

    @GetMapping("/receipts/{id}/items")
    public List<ReceiptItemDto> items(@PathVariable UUID id) {
        return receiptService.items(id);
    }

    @DeleteMapping("/receipts/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        receiptService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/receipt-items/{id}")
    public ReceiptItemDto updateItem(@PathVariable UUID id,
            @Valid @RequestBody UpdateReceiptItemRequest request) {
        return receiptService.updateItem(id, request);
    }

    /** Lineas sin categorizar, para la pantalla de categorias y reglas (CLAUDE §10). */
    @GetMapping("/receipt-items/uncategorized")
    public List<ReceiptItemDto> uncategorized() {
        return receiptService.uncategorized();
    }
}
