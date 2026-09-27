package mvega.dev.cuentas.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import mvega.dev.cuentas.analytics.dto.CategorySpendDto;
import mvega.dev.cuentas.analytics.dto.PeriodSpendDto;
import mvega.dev.cuentas.analytics.dto.PriceChangeDto;
import mvega.dev.cuentas.analytics.dto.PricePointDto;
import mvega.dev.cuentas.analytics.dto.ProductSpendDto;
import mvega.dev.cuentas.analytics.dto.SpendSummaryDto;
import mvega.dev.cuentas.shared.text.ProductNameNormalizer;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agregados de gasto. Se calculan en el backend: las reglas de dinero no se duplican en
 * el frontend (CLAUDE §11).
 *
 * <p>Todo importe se mueve en {@code numeric}/{@link BigDecimal}, nunca en coma flotante.
 * El precio unitario de un articulo al peso es su €/kg; el de un articulo por pieza es su
 * precio unitario impreso y, si no lo trae, el importe entre la cantidad.
 */
@Service
public class AnalyticsService {

    /** Granularidades admitidas. Se valida contra la lista para no inyectar SQL. */
    private static final Set<String> GRANULARITIES = Set.of("day", "week", "month");

    /** Precio unitario comparable entre compras, sea el articulo por pieza o al peso. */
    private static final String UNIT_PRICE_EXPR = """
            case when i.unit = 'KG' then i.price_per_kg
                 else coalesce(i.unit_price, i.line_total / nullif(i.quantity, 0)) end
            """;

    private final JdbcClient jdbcClient;

    AnalyticsService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional(readOnly = true)
    public SpendSummaryDto summary(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        Totals current = totalsBetween(start, end);

        // Periodo anterior de la misma duracion, para la comparacion del dashboard.
        long days = ChronoUnit.DAYS.between(start, end);
        Totals previous = totalsBetween(start.minusDays(days), start);

        BigDecimal average = current.count() == 0
                ? BigDecimal.ZERO
                : current.total().divide(BigDecimal.valueOf(current.count()), 2,
                        RoundingMode.HALF_UP);

        BigDecimal changePercent = null;
        if (previous.total().signum() != 0) {
            changePercent = current.total().subtract(previous.total())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previous.total(), 2, RoundingMode.HALF_UP);
        }

        return new SpendSummaryDto(from, to, current.total(), current.count(), average,
                previous.total(), changePercent);
    }

    private Totals totalsBetween(LocalDateTime start, LocalDateTime end) {
        return jdbcClient.sql("""
                select coalesce(sum(total_amount), 0) as total, count(*) as receipts
                from receipt
                where purchased_at >= :start and purchased_at < :end
                """)
                .param("start", start)
                .param("end", end)
                .query((rs, rowNum) -> new Totals(rs.getBigDecimal("total"),
                        rs.getLong("receipts")))
                .single();
    }

    @Transactional(readOnly = true)
    public List<CategorySpendDto> byCategory(LocalDate from, LocalDate to) {
        return jdbcClient.sql("""
                select c.id as category_id, c.name as category_name,
                       sum(i.line_total) as total
                from receipt_item i
                join receipt r on r.id = i.receipt_id
                left join category c on c.id = i.category_id
                where r.purchased_at >= :start and r.purchased_at < :end
                group by c.id, c.name
                order by total desc
                """)
                .param("start", from.atStartOfDay())
                .param("end", to.plusDays(1).atStartOfDay())
                .query((rs, rowNum) -> {
                    long categoryId = rs.getLong("category_id");
                    return new CategorySpendDto(
                            rs.wasNull() ? null : categoryId,
                            // Las lineas sin categorizar se agrupan de forma explicita
                            // en lugar de desaparecer del grafico.
                            rs.getString("category_name") == null
                                    ? "Sin categoría"
                                    : rs.getString("category_name"),
                            rs.getBigDecimal("total"));
                })
                .list();
    }

    @Transactional(readOnly = true)
    public List<PeriodSpendDto> byPeriod(LocalDate from, LocalDate to, String granularity) {
        String unit = granularity == null ? "month" : granularity.toLowerCase();
        if (!GRANULARITIES.contains(unit)) {
            throw new IllegalArgumentException(
                    "Granularidad no admitida: " + granularity + ". Usa day, week o month");
        }
        // date_trunc no acepta el campo como parametro, asi que se interpola, pero solo
        // tras validarlo contra la lista cerrada de arriba.
        String sql = """
                select to_char(date_trunc('%s', purchased_at), 'YYYY-MM-DD') as period,
                       sum(total_amount) as total
                from receipt
                where purchased_at >= :start and purchased_at < :end
                group by 1
                order by 1
                """.formatted(unit);

        return jdbcClient.sql(sql)
                .param("start", from.atStartOfDay())
                .param("end", to.plusDays(1).atStartOfDay())
                .query((rs, rowNum) -> new PeriodSpendDto(rs.getString("period"),
                        rs.getBigDecimal("total")))
                .list();
    }

    @Transactional(readOnly = true)
    public List<ProductSpendDto> topProducts(LocalDate from, LocalDate to, int limit) {
        return jdbcClient.sql("""
                select i.normalized_name,
                       sum(i.line_total) as total,
                       sum(i.quantity)   as total_quantity,
                       count(*)          as times_bought
                from receipt_item i
                join receipt r on r.id = i.receipt_id
                where r.purchased_at >= :start and r.purchased_at < :end
                  and i.normalized_name is not null
                group by i.normalized_name
                order by total desc
                limit :limit
                """)
                .param("start", from.atStartOfDay())
                .param("end", to.plusDays(1).atStartOfDay())
                .param("limit", limit)
                .query((rs, rowNum) -> new ProductSpendDto(rs.getString("normalized_name"),
                        rs.getBigDecimal("total"), rs.getBigDecimal("total_quantity"),
                        rs.getLong("times_bought")))
                .list();
    }

    /** Evolucion del precio unitario de un producto a lo largo del tiempo (CLAUDE §11). */
    @Transactional(readOnly = true)
    public List<PricePointDto> priceHistory(String productName) {
        String normalized = ProductNameNormalizer.normalize(productName);
        if (normalized == null) {
            return List.of();
        }
        return jdbcClient.sql("""
                select r.purchased_at, %s as unit_price
                from receipt_item i
                join receipt r on r.id = i.receipt_id
                where i.normalized_name = :name
                order by r.purchased_at
                """.formatted(UNIT_PRICE_EXPR))
                .param("name", normalized)
                .query((rs, rowNum) -> new PricePointDto(
                        rs.getTimestamp("purchased_at").toLocalDateTime(),
                        rs.getBigDecimal("unit_price")))
                .list();
    }

    /**
     * Productos cuyo precio ha cambiado entre las dos ultimas compras. Solo aparecen
     * cuando hay al menos dos observaciones, para no inventar tendencias (CLAUDE §10).
     */
    @Transactional(readOnly = true)
    public List<PriceChangeDto> recentPriceChanges(int limit) {
        return jdbcClient.sql("""
                with prices as (
                    select i.normalized_name as name,
                           r.purchased_at    as purchased_at,
                           %s                as unit_price,
                           row_number() over (partition by i.normalized_name
                                              order by r.purchased_at desc) as rn
                    from receipt_item i
                    join receipt r on r.id = i.receipt_id
                    where i.normalized_name is not null
                ),
                pairs as (
                    select name,
                           max(case when rn = 1 then unit_price end)   as current_price,
                           max(case when rn = 2 then unit_price end)   as previous_price,
                           max(case when rn = 1 then purchased_at end) as last_seen_at
                    from prices
                    where rn <= 2
                    group by name
                )
                select name, current_price, previous_price, last_seen_at
                from pairs
                where previous_price is not null
                  and current_price is not null
                  and current_price <> previous_price
                order by abs(current_price - previous_price) desc
                limit :limit
                """.formatted(UNIT_PRICE_EXPR))
                .param("limit", limit)
                .query((rs, rowNum) -> {
                    BigDecimal current = rs.getBigDecimal("current_price");
                    BigDecimal previous = rs.getBigDecimal("previous_price");
                    BigDecimal changePercent = previous.signum() == 0
                            ? null
                            : current.subtract(previous).multiply(BigDecimal.valueOf(100))
                                    .divide(previous, 2, RoundingMode.HALF_UP);
                    return new PriceChangeDto(rs.getString("name"), current, previous,
                            changePercent, rs.getTimestamp("last_seen_at").toLocalDateTime());
                })
                .list();
    }

    private record Totals(BigDecimal total, long count) {}
}
