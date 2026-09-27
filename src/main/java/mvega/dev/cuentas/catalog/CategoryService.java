package mvega.dev.cuentas.catalog;

import java.util.List;
import mvega.dev.cuentas.catalog.dto.CategoryDto;
import mvega.dev.cuentas.catalog.dto.CategoryRequest;
import mvega.dev.cuentas.receipt.ReceiptItemRepository;
import mvega.dev.cuentas.shared.error.ConflictException;
import mvega.dev.cuentas.shared.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRuleRepository productRuleRepository;
    private final ReceiptItemRepository receiptItemRepository;

    CategoryService(CategoryRepository categoryRepository,
            ProductRuleRepository productRuleRepository,
            ReceiptItemRepository receiptItemRepository) {
        this.categoryRepository = categoryRepository;
        this.productRuleRepository = productRuleRepository;
        this.receiptItemRepository = receiptItemRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> list() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(category -> new CategoryDto(category.getId(), category.getName()))
                .toList();
    }

    @Transactional
    public CategoryDto create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Ya existe una categoria llamada " + name);
        }
        Category saved = categoryRepository.save(Category.builder().name(name).build());
        return new CategoryDto(saved.getId(), saved.getName());
    }

    @Transactional
    public CategoryDto rename(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe la categoria " + id));
        String name = request.name().trim();
        if (!category.getName().equalsIgnoreCase(name)
                && categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Ya existe una categoria llamada " + name);
        }
        category.setName(name);
        return new CategoryDto(category.getId(), category.getName());
    }

    /**
     * Borrado. Se niega si la categoria esta en uso: perder de golpe la categorizacion
     * de lineas ya revisadas a mano seria peor que pedir al usuario que las reasigne.
     */
    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("No existe la categoria " + id);
        }
        long usedByItems = receiptItemRepository.countByCategoryId(id);
        if (usedByItems > 0) {
            throw new ConflictException(
                    "La categoria esta asignada a %d lineas. Reasignalas antes de borrarla"
                            .formatted(usedByItems));
        }
        long usedByRules = productRuleRepository.countByCategoryId(id);
        if (usedByRules > 0) {
            throw new ConflictException(
                    "La categoria la usan %d reglas de producto. Borralas antes"
                            .formatted(usedByRules));
        }
        categoryRepository.deleteById(id);
    }
}
