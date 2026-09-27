package mvega.dev.cuentas.receipt.importer;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mvega.dev.cuentas.catalog.Merchant;
import mvega.dev.cuentas.catalog.MerchantRepository;
import mvega.dev.cuentas.catalog.ProductRule;
import mvega.dev.cuentas.catalog.ProductRuleRepository;
import mvega.dev.cuentas.receipt.CategorySource;
import mvega.dev.cuentas.receipt.Receipt;
import mvega.dev.cuentas.receipt.ReceiptItem;
import mvega.dev.cuentas.receipt.ReceiptRepository;
import mvega.dev.cuentas.receipt.ReceiptTaxLine;
import mvega.dev.cuentas.receipt.SourceType;
import mvega.dev.cuentas.shared.text.ProductNameNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guarda un ticket interpretado y sus líneas.
 *
 * <p>Vive en su propio bean y no como método de {@link MercadonaImportService} porque
 * {@code @Transactional} se aplica mediante un proxy: una llamada interna del servicio a sí
 * mismo se salta el proxy y correría sin transacción. Separándolo, el ticket y sus líneas
 * entran juntos o no entran.
 */
@Service
public class ReceiptWriter {

    private final ReceiptRepository receiptRepository;
    private final MerchantRepository merchantRepository;
    private final ProductRuleRepository productRuleRepository;

    ReceiptWriter(ReceiptRepository receiptRepository, MerchantRepository merchantRepository,
            ProductRuleRepository productRuleRepository) {
        this.receiptRepository = receiptRepository;
        this.merchantRepository = merchantRepository;
        this.productRuleRepository = productRuleRepository;
    }

    /**
     * @throws DuplicateInvoiceException si ya hay un ticket con esa factura en el comercio,
     *         aunque el PDF sea distinto byte a byte
     */
    @Transactional
    public Receipt write(ParsedReceipt parsed, String sha256, String filename, SourceType channel,
            String parserVersion) {

        Merchant merchant = merchantRepository.findByName(parsed.merchantName())
                .orElseGet(() -> merchantRepository.save(Merchant.builder()
                        .name(parsed.merchantName())
                        .taxId(parsed.taxId())
                        .build()));

        if (parsed.invoiceNumber() != null) {
            Optional<Receipt> byInvoice = receiptRepository
                    .findByMerchantIdAndInvoiceNumber(merchant.getId(), parsed.invoiceNumber());
            if (byInvoice.isPresent()) {
                throw new DuplicateInvoiceException(parsed.invoiceNumber(), byInvoice.get().getId());
            }
        }

        Receipt receipt = Receipt.builder()
                .merchant(merchant)
                .purchasedAt(parsed.purchasedAt())
                .storeName(parsed.storeName())
                .storeAddress(parsed.storeAddress())
                .storePhone(parsed.storePhone())
                .operationNumber(parsed.operationNumber())
                .invoiceNumber(parsed.invoiceNumber())
                .currency(parsed.currency())
                .totalAmount(parsed.totalAmount())
                .paymentMethod(parsed.paymentMethod())
                .cardLast4(parsed.cardLast4())
                .sourceType(channel)
                .sourceFilename(filename)
                .contentHash(sha256)
                .parserVersion(parserVersion)
                .parsingStatus(parsed.status())
                .rawText(parsed.warnings().isEmpty() ? null : String.join("\n", parsed.warnings()))
                .importedAt(Instant.now())
                .build();

        for (ParsedReceiptItem item : parsed.items()) {
            receipt.addItem(ReceiptItem.builder()
                    .lineNumber(item.lineNumber())
                    .rawDescription(item.rawDescription())
                    .normalizedName(ProductNameNormalizer.normalize(item.rawDescription()))
                    .quantity(item.quantity())
                    .unit(item.unit())
                    .unitPrice(item.unitPrice())
                    .weightKg(item.weightKg())
                    .pricePerKg(item.pricePerKg())
                    .lineTotal(item.lineTotal())
                    .categorySource(CategorySource.UNASSIGNED)
                    .build());
        }

        for (ParsedTaxLine taxLine : parsed.taxLines()) {
            receipt.addTaxLine(ReceiptTaxLine.builder()
                    .ratePercent(taxLine.ratePercent())
                    .taxableBase(taxLine.taxableBase())
                    .taxAmount(taxLine.taxAmount())
                    .totalAmount(taxLine.totalAmount())
                    .build());
        }

        applyCategoryRules(receipt, merchant);
        return receiptRepository.save(receipt);
    }

    /**
     * Asigna categoría a las líneas cuyo nombre normalizado ya tiene una regla guardada. El
     * resto queda sin asignar para que lo decida el usuario. Reglas deterministas, sin IA
     * (CLAUDE §11).
     */
    private void applyCategoryRules(Receipt receipt, Merchant merchant) {
        List<ProductRule> rules = productRuleRepository.findActiveByMerchant(merchant.getId());
        if (rules.isEmpty()) {
            return;
        }
        Map<String, ProductRule> byMatchText = new HashMap<>();
        for (ProductRule rule : rules) {
            byMatchText.put(rule.getMatchText(), rule);
        }
        for (ReceiptItem item : receipt.getItems()) {
            ProductRule rule = byMatchText.get(item.getNormalizedName());
            if (rule != null) {
                item.setCategory(rule.getCategory());
                item.setCategorySource(CategorySource.RULE);
                if (rule.getNormalizedName() != null) {
                    item.setNormalizedName(rule.getNormalizedName());
                }
            }
        }
    }

    /** Ya existe un ticket con esa factura, aunque el PDF no sea el mismo fichero. */
    public static class DuplicateInvoiceException extends RuntimeException {

        private final java.util.UUID existingReceiptId;

        DuplicateInvoiceException(String invoiceNumber, java.util.UUID existingReceiptId) {
            super("Ya existe un ticket con la factura " + invoiceNumber);
            this.existingReceiptId = existingReceiptId;
        }

        public java.util.UUID getExistingReceiptId() {
            return existingReceiptId;
        }
    }
}
