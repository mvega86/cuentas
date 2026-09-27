package mvega.dev.cuentas.receipt.importer;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ImportRecordRepository extends JpaRepository<ImportRecord, Long> {

    @Query("""
            select r from ImportRecord r
            left join fetch r.receipt
            order by r.startedAt desc, r.id desc
            """)
    Page<ImportRecord> findHistory(Pageable pageable);

    @Query("select r from ImportRecord r left join fetch r.receipt where r.id = :id")
    Optional<ImportRecord> findDetailById(Long id);

    Optional<ImportRecord> findFirstByProcessingStatusOrderByFinishedAtDesc(ImportStatus status);

    long countByProcessingStatus(ImportStatus status);
}
