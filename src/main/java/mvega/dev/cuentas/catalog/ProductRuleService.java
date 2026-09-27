package mvega.dev.cuentas.catalog;

import java.time.Instant;
import java.util.List;
import mvega.dev.cuentas.catalog.dto.ProductRuleDto;
import mvega.dev.cuentas.catalog.dto.ProductRuleRequest;
import mvega.dev.cuentas.receipt.importer.MercadonaTicketParser;
import mvega.dev.cuentas.shared.error.ConflictException;
import mvega.dev.cuentas.shared.error.NotFoundException;
import mvega.dev.cuentas.shared.text.ProductNameNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestion de las reglas de categorizacion de productos (CLAUDE §10, pantalla 5). */
@Service
public class ProductRuleService {

    private final ProductRuleRepository productRuleRepository;
    private final CategoryRepository categoryRepository;
    private final MerchantRepository merchantRepository;

    ProductRuleService(ProductRuleRepository productRuleRepository,
            CategoryRepository categoryRepository, MerchantRepository merchantRepository) {
        this.productRuleRepository = productRuleRepository;
        this.categoryRepository = categoryRepository;
        this.merchantRepository = merchantRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductRuleDto> list() {
        return productRuleRepository.findAllByOrderByMatchTextAsc().stream()
                .map(ProductRuleService::toDto)
                .toList();
    }

    @Transactional
    public ProductRuleDto create(ProductRuleRequest request) {
        Merchant merchant = merchantRepository.findByName(MercadonaTicketParser.MERCHANT_NAME)
                .orElseThrow(() -> new NotFoundException("No existe el comercio Mercadona"));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException(
                        "No existe la categoria " + request.categoryId()));

        String matchText = ProductNameNormalizer.normalize(request.matchText());
        productRuleRepository.findByMerchantIdAndMatchText(merchant.getId(), matchText)
                .ifPresent(existing -> {
                    throw new ConflictException("Ya hay una regla para " + matchText);
                });

        ProductRule saved = productRuleRepository.save(ProductRule.builder()
                .merchant(merchant)
                .matchText(matchText)
                .normalizedName(matchText)
                .category(category)
                .active(request.active() == null || request.active())
                .createdAt(Instant.now())
                .build());
        return toDto(saved);
    }

    @Transactional
    public ProductRuleDto update(Long id, ProductRuleRequest request) {
        ProductRule rule = productRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe la regla " + id));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException(
                        "No existe la categoria " + request.categoryId()));
        rule.setCategory(category);
        rule.setMatchText(ProductNameNormalizer.normalize(request.matchText()));
        if (request.active() != null) {
            rule.setActive(request.active());
        }
        return toDto(rule);
    }

    @Transactional
    public void delete(Long id) {
        if (!productRuleRepository.existsById(id)) {
            throw new NotFoundException("No existe la regla " + id);
        }
        productRuleRepository.deleteById(id);
    }

    private static ProductRuleDto toDto(ProductRule rule) {
        return new ProductRuleDto(rule.getId(), rule.getMatchText(), rule.getNormalizedName(),
                rule.getCategory().getId(), rule.getCategory().getName(), rule.isActive());
    }
}
