package mvega.dev.cuentas.receipt;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, UUID> {

    List<ReceiptItem> findByReceiptIdOrderByLineNumberAsc(UUID receiptId);

    /** Lineas sin categoria, para la pantalla de pendientes de categorizar (CLAUDE §10). */
    @Query("""
            select i from ReceiptItem i
            join fetch i.receipt r
            where i.category is null
            order by r.purchasedAt desc, i.lineNumber asc
            """)
    List<ReceiptItem> findUncategorized();

    /** Cuantas lineas usan una categoria. Lo consulta el borrado de categorias. */
    long countByCategoryId(Long categoryId);
}
