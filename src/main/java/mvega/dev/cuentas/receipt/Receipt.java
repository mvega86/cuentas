package mvega.dev.cuentas.receipt;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mvega.dev.cuentas.catalog.Merchant;

/**
 * Un ticket de compra importado (CLAUDE §5).
 *
 * <p>El {@code @ManyToOne} hacia el comercio va sin cascada: un ticket jamas debe
 * modificar ni borrar el comercio (CLAUDE §2). Las cascadas solo van de padre a hijo,
 * hacia lineas y desglose de IVA, que no existen fuera del ticket.
 */
@Entity
@Table(name = "receipt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    /** Fecha y hora impresas en el ticket. Sin zona: es hora local de la tienda. */
    @Column(name = "purchased_at", nullable = false)
    private LocalDateTime purchasedAt;

    @Column(name = "store_name", length = 160)
    private String storeName;

    @Column(name = "store_address", length = 240)
    private String storeAddress;

    @Column(name = "store_phone", length = 40)
    private String storePhone;

    @Column(name = "operation_number", length = 60)
    private String operationNumber;

    @Column(name = "invoice_number", length = 60)
    private String invoiceNumber;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_method", length = 60)
    private String paymentMethod;

    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16)
    private SourceType sourceType;

    @Column(name = "source_filename", length = 260)
    private String sourceFilename;

    /** SHA-256 del PDF original. Red de seguridad antiduplicados. */
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "parser_version", nullable = false, length = 40)
    private String parserVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "parsing_status", nullable = false, length = 16)
    private ParsingStatus parsingStatus;

    /** Texto extraido del PDF. Se conserva en esta fase para poder depurar (CLAUDE §5). */
    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber asc")
    @Builder.Default
    private List<ReceiptItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ratePercent asc")
    @Builder.Default
    private List<ReceiptTaxLine> taxLines = new ArrayList<>();

    /** Añade una linea manteniendo las dos puntas de la relacion coherentes. */
    public void addItem(ReceiptItem item) {
        items.add(item);
        item.setReceipt(this);
    }

    /** Añade una fila del desglose de IVA manteniendo la relacion coherente. */
    public void addTaxLine(ReceiptTaxLine taxLine) {
        taxLines.add(taxLine);
        taxLine.setReceipt(this);
    }
}
