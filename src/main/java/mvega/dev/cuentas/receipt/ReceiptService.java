package mvega.dev.cuentas.receipt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import mvega.dev.cuentas.catalog.Category;
import mvega.dev.cuentas.catalog.CategoryRepository;
import mvega.dev.cuentas.catalog.ProductRule;
import mvega.dev.cuentas.catalog.ProductRuleRepository;
import mvega.dev.cuentas.receipt.dto.ReceiptDetailDto;
import mvega.dev.cuentas.receipt.dto.ReceiptItemDto;
import mvega.dev.cuentas.receipt.dto.ReceiptSummaryDto;
import mvega.dev.cuentas.receipt.dto.UpdateReceiptItemRequest;
import mvega.dev.cuentas.shared.error.NotFoundException;
import mvega.dev.cuentas.shared.text.ProductNameNormalizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceiptService {

    /** Limites por defecto cuando el usuario no filtra por fecha. */
    private static final LocalDateTime MIN = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime MAX = LocalDateTime.of(9999, 12, 31, 23, 59);

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRuleRepository productRuleRepository;

    ReceiptService(ReceiptRepository receiptRepository, ReceiptItemRepository receiptItemRepository,
            CategoryRepository categoryRepository, ProductRuleRepository productRuleRepository) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.categoryRepository = categoryRepository;
        this.productRuleRepository = productRuleRepository;
    }

    @Transactional(readOnly = true)
    public Page<ReceiptSummaryDto> list(LocalDateTime from, LocalDateTime to, int page, int size) {
        return receiptRepository.findSummaries(from == null ? MIN : from, to == null ? MAX : to,
                PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public ReceiptDetailDto detail(UUID id) {
        Receipt receipt = receiptRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("No existe el ticket " + id));
        return ReceiptMapper.toDetail(receipt);
    }

    @Transactional(readOnly = true)
    public List<ReceiptItemDto> items(UUID receiptId) {
        if (!receiptRepository.existsById(receiptId)) {
            throw new NotFoundException("No existe el ticket " + receiptId);
        }
        return ReceiptMapper.toItems(receiptItemRepository.findByReceiptIdOrderByLineNumberAsc(receiptId));
    }

    @Transactional
    public void delete(UUID id) {
        if (!receiptRepository.existsById(id)) {
            throw new NotFoundException("No existe el ticket " + id);
        }
        receiptRepository.deleteById(id);
    }

    /**
     * Edicion manual de una linea: nombre normalizado y categoria.
     *
     * <p>Si se pide guardar la decision como regla, la categoria elegida se aplicara
     * automaticamente a ese producto en las importaciones futuras (CLAUDE §5, §10).
     */
    @Transactional
    public ReceiptItemDto updateItem(UUID itemId, UpdateReceiptItemRequest request) {
        ReceiptItem item = receiptItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("No existe la linea " + itemId));

        if (request.normalizedName() != null) {
            item.setNormalizedName(ProductNameNormalizer.normalize(request.normalizedName()));
        }

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException(
                            "No existe la categoria " + request.categoryId()));
            item.setCategory(category);
            item.setCategorySource(CategorySource.MANUAL);

            if (request.saveAsRule()) {
                saveRule(item, category);
            }
        }

        return ReceiptMapper.toItem(item);
    }

    private void saveRule(ReceiptItem item, Category category) {
        String matchText = item.getNormalizedName() != null
                ? item.getNormalizedName()
                : ProductNameNormalizer.normalize(item.getRawDescription());
        if (matchText == null) {
            return;
        }
        var merchant = item.getReceipt().getMerchant();
        productRuleRepository.findByMerchantIdAndMatchText(merchant.getId(), matchText)
                .ifPresentOrElse(existing -> {
                    existing.setCategory(category);
                    existing.setActive(true);
                }, () -> productRuleRepository.save(ProductRule.builder()
                        .merchant(merchant)
                        .matchText(matchText)
                        .normalizedName(matchText)
                        .category(category)
                        .active(true)
                        .createdAt(Instant.now())
                        .build()));
    }

    @Transactional(readOnly = true)
    public List<ReceiptItemDto> uncategorized() {
        return ReceiptMapper.toItems(receiptItemRepository.findUncategorized());
    }
}
