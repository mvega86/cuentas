package mvega.dev.cuentas.analytics;

import java.time.LocalDate;
import java.util.List;
import mvega.dev.cuentas.analytics.dto.CategorySpendDto;
import mvega.dev.cuentas.analytics.dto.PeriodSpendDto;
import mvega.dev.cuentas.analytics.dto.PriceChangeDto;
import mvega.dev.cuentas.analytics.dto.PricePointDto;
import mvega.dev.cuentas.analytics.dto.ProductSpendDto;
import mvega.dev.cuentas.analytics.dto.SpendSummaryDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de analitica del dashboard (CLAUDE §9, §10, §11). */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public SpendSummaryDto summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Range range = Range.of(from, to);
        return analyticsService.summary(range.from(), range.to());
    }

    @GetMapping("/by-category")
    public List<CategorySpendDto> byCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Range range = Range.of(from, to);
        return analyticsService.byCategory(range.from(), range.to());
    }

    @GetMapping("/by-period")
    public List<PeriodSpendDto> byPeriod(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "month") String granularity) {
        Range range = Range.of(from, to);
        return analyticsService.byPeriod(range.from(), range.to(), granularity);
    }

    @GetMapping("/products")
    public List<ProductSpendDto> products(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        Range range = Range.of(from, to);
        return analyticsService.topProducts(range.from(), range.to(), Math.min(limit, 100));
    }

    @GetMapping("/products/price-history")
    public List<PricePointDto> priceHistory(@RequestParam String product) {
        return analyticsService.priceHistory(product);
    }

    @GetMapping("/price-changes")
    public List<PriceChangeDto> priceChanges(@RequestParam(defaultValue = "5") int limit) {
        return analyticsService.recentPriceChanges(Math.min(limit, 50));
    }

    /** Sin fechas, el dashboard muestra los ultimos 30 dias (CLAUDE §10). */
    private record Range(LocalDate from, LocalDate to) {
        static Range of(LocalDate from, LocalDate to) {
            LocalDate end = to != null ? to : LocalDate.now();
            LocalDate start = from != null ? from : end.minusDays(29);
            if (start.isAfter(end)) {
                throw new IllegalArgumentException("La fecha inicial es posterior a la final");
            }
            return new Range(start, end);
        }
    }
}
