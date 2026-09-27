package mvega.dev.cuentas.receipt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mvega.dev.cuentas.receipt.dto.ReceiptSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    // --- Las dos capas antiduplicado (CLAUDE §5) ---
    // La tercera capa, los identificadores de Gmail, desaparece con la integración
    // directa con Gmail: el backend ya solo ve ficheros.

    Optional<Receipt> findByContentHash(String contentHash);

    Optional<Receipt> findByMerchantIdAndInvoiceNumber(Long merchantId, String invoiceNumber);

    /**
     * Listado paginado. Devuelve directamente el DTO con el numero de lineas ya contado,
     * en lugar de cargar las lineas de cada ticket y provocar un N+1.
     *
     * <p>El orden va en la consulta, asi que el {@link Pageable} se pasa sin criterio de
     * ordenacion.
     */
    @Query(value = """
            select new mvega.dev.cuentas.receipt.dto.ReceiptSummaryDto(
                r.id, r.purchasedAt, m.name, r.totalAmount, count(i.id), r.parsingStatus,
                r.sourceType)
            from Receipt r
            join r.merchant m
            left join r.items i
            where r.purchasedAt >= :from and r.purchasedAt < :to
            group by r.id, r.purchasedAt, m.name, r.totalAmount, r.parsingStatus, r.sourceType
            order by r.purchasedAt desc
            """,
            countQuery = """
            select count(r) from Receipt r
            where r.purchasedAt >= :from and r.purchasedAt < :to
            """)
    Page<ReceiptSummaryDto> findSummaries(@Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to, Pageable pageable);

    /** Detalle con lineas y categorias en una sola consulta. */
    @Query("""
            select distinct r from Receipt r
            join fetch r.merchant
            left join fetch r.items i
            left join fetch i.category
            where r.id = :id
            """)
    Optional<Receipt> findDetailById(@Param("id") UUID id);

    @Query("""
            select new mvega.dev.cuentas.receipt.dto.ReceiptSummaryDto(
                r.id, r.purchasedAt, m.name, r.totalAmount, count(i.id), r.parsingStatus,
                r.sourceType)
            from Receipt r
            join r.merchant m
            left join r.items i
            group by r.id, r.purchasedAt, m.name, r.totalAmount, r.parsingStatus, r.sourceType
            order by r.purchasedAt desc
            """)
    List<ReceiptSummaryDto> findRecent(Pageable pageable);
}
