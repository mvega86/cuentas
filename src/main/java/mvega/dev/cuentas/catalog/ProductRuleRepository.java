package mvega.dev.cuentas.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRuleRepository extends JpaRepository<ProductRule, Long> {

    Optional<ProductRule> findByMerchantIdAndMatchText(Long merchantId, String matchText);

    @Query("select r from ProductRule r join fetch r.category where r.merchant.id = :merchantId and r.active = true")
    List<ProductRule> findActiveByMerchant(Long merchantId);

    List<ProductRule> findAllByOrderByMatchTextAsc();

    long countByCategoryId(Long categoryId);
}
