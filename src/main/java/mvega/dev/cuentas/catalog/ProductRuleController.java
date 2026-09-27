package mvega.dev.cuentas.catalog;

import jakarta.validation.Valid;
import java.util.List;
import mvega.dev.cuentas.catalog.dto.ProductRuleDto;
import mvega.dev.cuentas.catalog.dto.ProductRuleRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product-rules")
public class ProductRuleController {

    private final ProductRuleService productRuleService;

    ProductRuleController(ProductRuleService productRuleService) {
        this.productRuleService = productRuleService;
    }

    @GetMapping
    public List<ProductRuleDto> list() {
        return productRuleService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductRuleDto create(@Valid @RequestBody ProductRuleRequest request) {
        return productRuleService.create(request);
    }

    @PatchMapping("/{id}")
    public ProductRuleDto update(@PathVariable Long id,
            @Valid @RequestBody ProductRuleRequest request) {
        return productRuleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productRuleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
