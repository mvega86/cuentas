package mvega.dev.cuentas.receipt;

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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mvega.dev.cuentas.catalog.Category;

/**
 * Una linea de compra del ticket (CLAUDE §5).
 *
 * <p>Las descripciones se repiten dentro de un mismo ticket (por ejemplo dos
 * {@code TORREZNO} a precios distintos), asi que la linea se identifica por
 * {@code lineNumber}, nunca por la descripcion.
 */
@Entity
@Table(name = "receipt_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    /** Descripcion exactamente como aparece en el ticket. No se toca (CLAUDE §6). */
    @Column(name = "raw_description", nullable = false, length = 240)
    private String rawDescription;

    @Column(name = "normalized_name", length = 240)
    private String normalizedName;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ItemUnit unit;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "weight_kg", precision = 10, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "price_per_kg", precision = 12, scale = 2)
    private BigDecimal pricePerKg;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_source", nullable = false, length = 16)
    private CategorySource categorySource;
}
