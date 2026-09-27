package mvega.dev.cuentas.receipt;

import java.util.List;
import mvega.dev.cuentas.receipt.dto.ReceiptDetailDto;
import mvega.dev.cuentas.receipt.dto.ReceiptItemDto;
import mvega.dev.cuentas.receipt.dto.ReceiptTaxLineDto;

/**
 * Paso de entidad a DTO. Se hace a mano y de forma explicita: las entidades JPA no
 * salen nunca por la API (CLAUDE §4 de las reglas, §9).
 */
public final class ReceiptMapper {

    private ReceiptMapper() {
    }

    public static ReceiptDetailDto toDetail(Receipt receipt) {
        return new ReceiptDetailDto(
                receipt.getId(),
                receipt.getPurchasedAt(),
                receipt.getMerchant().getName(),
                receipt.getStoreAddress(),
                receipt.getStorePhone(),
                receipt.getOperationNumber(),
                receipt.getInvoiceNumber(),
                receipt.getCurrency(),
                receipt.getTotalAmount(),
                receipt.getPaymentMethod(),
                receipt.getCardLast4(),
                receipt.getSourceType(),
                receipt.getSourceFilename(),
                receipt.getParsingStatus(),
                receipt.getParserVersion(),
                receipt.getRawText(),
                receipt.getImportedAt(),
                receipt.getItems().stream().map(ReceiptMapper::toItem).toList(),
                receipt.getTaxLines().stream().map(ReceiptMapper::toTaxLine).toList());
    }

    public static ReceiptItemDto toItem(ReceiptItem item) {
        return new ReceiptItemDto(
                item.getId(),
                item.getLineNumber(),
                item.getRawDescription(),
                item.getNormalizedName(),
                item.getQuantity(),
                item.getUnit(),
                item.getUnitPrice(),
                item.getWeightKg(),
                item.getPricePerKg(),
                item.getLineTotal(),
                item.getCategory() == null ? null : item.getCategory().getId(),
                item.getCategory() == null ? null : item.getCategory().getName(),
                item.getCategorySource());
    }

    public static List<ReceiptItemDto> toItems(List<ReceiptItem> items) {
        return items.stream().map(ReceiptMapper::toItem).toList();
    }

    public static ReceiptTaxLineDto toTaxLine(ReceiptTaxLine taxLine) {
        return new ReceiptTaxLineDto(taxLine.getRatePercent(), taxLine.getTaxableBase(),
                taxLine.getTaxAmount(), taxLine.getTotalAmount());
    }
}
