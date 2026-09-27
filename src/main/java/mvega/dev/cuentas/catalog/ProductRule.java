package mvega.dev.cuentas.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Regla que recuerda la categoria elegida para un producto, para no tener que
 * clasificarlo de nuevo en cada compra (CLAUDE §5).
 *
 * <p>Sin cascada en los {@code @ManyToOne}: la regla nunca debe poder modificar ni
 * borrar el comercio ni la categoria a los que apunta (CLAUDE §2).
 */
@Entity
@Table(name = "product_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    /** Texto normalizado con el que se compara la descripcion del ticket. */
    @Column(name = "match_text", nullable = false, length = 240)
    private String matchText;

    @Column(name = "normalized_name", length = 240)
    private String normalizedName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
