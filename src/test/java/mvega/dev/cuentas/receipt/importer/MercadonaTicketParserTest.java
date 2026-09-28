package mvega.dev.cuentas.receipt.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import mvega.dev.cuentas.receipt.ItemUnit;
import mvega.dev.cuentas.receipt.ParsingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests del parser de Mercadona contra los ocho PDFs de muestra (CLAUDE §6, §15).
 *
 * <p>Son tests unitarios puros: ni contexto de Spring ni base de datos. El parser recibe
 * texto y devuelve un {@link ParsedReceipt}, asi que se puede comprobar de forma aislada.
 */
class MercadonaTicketParserTest {

    private final MercadonaTicketParser parser = new MercadonaTicketParser();

    /** Lo que cada ticket de muestra debe producir. Valores leidos de los propios PDFs. */
    record Expected(String file, LocalDateTime purchasedAt, String invoiceNumber,
            String total, int itemCount, int weightItemCount) {}

    static Stream<Expected> samples() {
        return Stream.of(
                new Expected(TicketFixtures.TICKET_3_77,
                        LocalDateTime.of(2026, 9, 12, 12, 52), "2345-013-669464", "3.77", 2, 0),
                new Expected(TicketFixtures.TICKET_17_72,
                        LocalDateTime.of(2026, 9, 14, 9, 32), "2345-011-172219",
                        "17.72", 6, 0),
                new Expected(TicketFixtures.TICKET_33_63,
                        LocalDateTime.of(2026, 9, 16, 10, 39), "2345-013-670570",
                        "33.63", 12, 2),
                new Expected(TicketFixtures.TICKET_3_68,
                        LocalDateTime.of(2026, 9, 18, 9, 41), "2345-015-182245",
                        "3.68", 2, 0),
                new Expected(TicketFixtures.TICKET_19_00,
                        LocalDateTime.of(2026, 9, 19, 19, 52), "2345-014-814081",
                        "19.00", 11, 0),
                new Expected(TicketFixtures.TICKET_48_75,
                        LocalDateTime.of(2026, 9, 19, 11, 45), "2345-014-813852",
                        "48.75", 22, 0),
                new Expected(TicketFixtures.TICKET_2_45,
                        LocalDateTime.of(2026, 9, 22, 20, 22), "2345-010-980458",
                        "2.45", 2, 0),
                new Expected(TicketFixtures.TICKET_73_70,
                        LocalDateTime.of(2026, 9, 26, 14, 4), "2345-012-184052",
                        "73.70", 30, 1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("samples")
    @DisplayName("cada ticket de muestra se parsea completo y sin avisos")
    void parsesEverySample(Expected expected) {
        ParsedReceipt receipt = parser.parse(TicketFixtures.textOf(expected.file()));

        assertThat(receipt.purchasedAt()).isEqualTo(expected.purchasedAt());
        assertThat(receipt.invoiceNumber()).isEqualTo(expected.invoiceNumber());
        assertThat(receipt.operationNumber()).isEqualTo("1000001");
        assertThat(receipt.totalAmount()).isEqualByComparingTo(new BigDecimal(expected.total()));
        assertThat(receipt.items()).hasSize(expected.itemCount());
        assertThat(receipt.warnings()).isEmpty();
        assertThat(receipt.status()).isEqualTo(ParsingStatus.PARSED);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("samples")
    @DisplayName("la suma de los importes de linea cuadra con el total del ticket")
    void lineSumMatchesTotal(Expected expected) {
        ParsedReceipt receipt = parser.parse(TicketFixtures.textOf(expected.file()));

        BigDecimal sum = receipt.items().stream()
                .map(ParsedReceiptItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(sum).isEqualByComparingTo(receipt.totalAmount());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("samples")
    @DisplayName("los articulos al peso llevan peso y precio por kilo, y el resto no")
    void weightItemsAreDetected(Expected expected) {
        ParsedReceipt receipt = parser.parse(TicketFixtures.textOf(expected.file()));

        List<ParsedReceiptItem> weighed = receipt.items().stream()
                .filter(item -> item.unit() == ItemUnit.KG)
                .toList();

        assertThat(weighed).hasSize(expected.weightItemCount());
        assertThat(weighed).allSatisfy(item -> {
            assertThat(item.weightKg()).isNotNull().isPositive();
            assertThat(item.pricePerKg()).isNotNull().isPositive();
            // En un articulo al peso la cantidad ES el peso, para que
            // cantidad x precio/kg reproduzca el importe de la linea.
            assertThat(item.quantity()).isEqualByComparingTo(item.weightKg());
            assertThat(item.unitPrice()).isNull();
        });
        assertThat(receipt.items()).filteredOn(item -> item.unit() == ItemUnit.UNIT)
                .allSatisfy(item -> {
                    assertThat(item.weightKg()).isNull();
                    assertThat(item.pricePerKg()).isNull();
                });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("samples")
    @DisplayName("los datos de la tienda y del pago se leen del contenido del PDF")
    void readsStoreAndPaymentData(Expected expected) {
        ParsedReceipt receipt = parser.parse(TicketFixtures.textOf(expected.file()));

        assertThat(receipt.merchantName()).isEqualTo("Mercadona");
        assertThat(receipt.taxId()).isEqualTo("A-46103834");
        assertThat(receipt.storeAddress()).isEqualTo("CL EJEMPLO, 1, 00000 CIUDAD");
        assertThat(receipt.storePhone()).isEqualTo("000000000");
        assertThat(receipt.currency()).isEqualTo("EUR");
        assertThat(receipt.paymentMethod()).isEqualTo("TARJETA BANCARIA");
        assertThat(receipt.cardLast4()).isEqualTo("0000");
    }

    @Test
    @DisplayName("el articulo al peso se asocia a su linea anterior, no a una propia")
    void weightLineAttachesToPreviousItem() {
        ParsedReceipt receipt = parser.parse(TicketFixtures.textOf(TicketFixtures.TICKET_73_70));

        ParsedReceiptItem pimiento = receipt.items().stream()
                .filter(item -> item.rawDescription().equals("PIMIENTO ROJO"))
                .findFirst()
                .orElseThrow();

        assertThat(pimiento.unit()).isEqualTo(ItemUnit.KG);
        assertThat(pimiento.weightKg()).isEqualByComparingTo(new BigDecimal("0.718"));
        assertThat(pimiento.pricePerKg()).isEqualByComparingTo(new BigDecimal("2.50"));
        assertThat(pimiento.lineTotal()).isEqualByComparingTo(new BigDecimal("1.80"));
        // La linea "0,718 kg ..." no debe generar un articulo suelto.
        assertThat(receipt.items()).noneMatch(item -> item.rawDescription().contains("kg"));
    }

    @Test
    @DisplayName("los dos articulos al peso del mismo ticket no se mezclan entre si")
    void twoWeightItemsInTheSameReceipt() {
        ParsedReceipt receipt = parser.parse(TicketFixtures.textOf(TicketFixtures.TICKET_33_63));

        ParsedReceiptItem platano = itemNamed(receipt, "PLATANO");
        ParsedReceiptItem yuca = itemNamed(receipt, "YUCA GRANEL");

        assertThat(platano.weightKg()).isEqualByComparingTo(new BigDecimal("1.084"));
        assertThat(platano.pricePerKg()).isEqualByComparingTo(new BigDecimal("1.80"));
        assertThat(platano.lineTotal()).isEqualByComparingTo(new BigDecimal("1.95"));

        assertThat(yuca.weightKg()).isEqualByComparingTo(new BigDecimal("1.664"));
        assertThat(yuca.pricePerKg()).isEqualByComparingTo(new BigDecimal("3.30"));
        assertThat(yuca.lineTotal()).isEqualByComparingTo(new BigDecimal("5.49"));
    }

    @Test
    @DisplayName("una linea con precio unitario separa precio unitario e importe")
    void readsUnitPriceWhenPrinted() {
        ParsedReceipt receipt = parser.parse(TicketFixtures.textOf(TicketFixtures.TICKET_73_70));

        ParsedReceiptItem arroz = itemNamed(receipt, "ARROZ REDONDO");
        assertThat(arroz.quantity()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(arroz.unitPrice()).isEqualByComparingTo(new BigDecimal("1.15"));
        assertThat(arroz.lineTotal()).isEqualByComparingTo(new BigDecimal("2.30"));

        // Sin precio unitario impreso, el campo queda vacio en lugar de inventarse.
        ParsedReceiptItem aceite = itemNamed(receipt, "ACEITE GIRASOL");
        assertThat(aceite.unitPrice()).isNull();
        assertThat(aceite.lineTotal()).isEqualByComparingTo(new BigDecimal("1.85"));
    }

    @Test
    @DisplayName("una descripcion con % no se confunde con un precio unitario")
    void percentInDescriptionIsNotAnAmount() {
        ParsedReceipt receipt = parser.parse(TicketFixtures.textOf(TicketFixtures.TICKET_73_70));

        ParsedReceiptItem champu = itemNamed(receipt, "CHAMPU 0%");
        assertThat(champu.unitPrice()).isNull();
        assertThat(champu.lineTotal()).isEqualByComparingTo(new BigDecimal("1.90"));
    }

    @Test
    @DisplayName("descripciones repetidas en un ticket se conservan como lineas distintas")
    void repeatedDescriptionsStaySeparateLines() {
        ParsedReceipt receipt = parser.parse(TicketFixtures.textOf(TicketFixtures.TICKET_33_63));

        List<ParsedReceiptItem> torreznos = receipt.items().stream()
                .filter(item -> item.rawDescription().equals("TORREZNO"))
                .toList();

        assertThat(torreznos).hasSize(2);
        assertThat(torreznos).extracting(ParsedReceiptItem::lineTotal)
                .containsExactly(new BigDecimal("3.13"), new BigDecimal("3.33"));
        assertThat(torreznos).extracting(ParsedReceiptItem::lineNumber).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("el desglose de IVA se lee y cuadra con el total")
    void readsTaxBreakdown() {
        ParsedReceipt receipt = parser.parse(TicketFixtures.textOf(TicketFixtures.TICKET_73_70));

        assertThat(receipt.taxLines()).hasSize(3);
        assertThat(receipt.taxLines()).extracting(ParsedTaxLine::ratePercent)
                .containsExactly(new BigDecimal("4"), new BigDecimal("10"), new BigDecimal("21"));

        BigDecimal taxTotals = receipt.taxLines().stream()
                .map(ParsedTaxLine::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(taxTotals).isEqualByComparingTo(receipt.totalAmount());
    }

    @Test
    @DisplayName("todas las lineas van numeradas de forma consecutiva desde 1")
    void lineNumbersAreConsecutive() {
        ParsedReceipt receipt = parser.parse(TicketFixtures.textOf(TicketFixtures.TICKET_73_70));

        assertThat(receipt.items()).extracting(ParsedReceiptItem::lineNumber)
                .containsExactlyElementsOf(
                        java.util.stream.IntStream.rangeClosed(1, receipt.items().size())
                                .boxed()
                                .toList());
    }

    // --- Reconocimiento del documento, antes de interpretar nada ---

    @ParameterizedTest(name = "{0}")
    @MethodSource("samples")
    @DisplayName("los ocho tickets de Mercadona se reconocen")
    void recognisesEveryRealTicket(Expected expected) {
        assertThat(parser.supports(TicketFixtures.textOf(expected.file()))).isTrue();
    }

    @Test
    @DisplayName("un texto vacío o nulo no se reconoce")
    void emptyTextIsNotRecognised() {
        assertThat(parser.supports(null)).isFalse();
        assertThat(parser.supports("")).isFalse();
        assertThat(parser.supports("   \n  \n ")).isFalse();
    }

    @Test
    @DisplayName("un ticket de otro comercio no se reconoce, aunque tenga TOTAL, IVA e importes")
    void otherMerchantIsNotRecognised() {
        String otro = """
                SUPERMERCADOS EJEMPLO, S.L.   B-00000000
                CL OTRA, 2
                FECHA: 26/09/2026 10:00
                Cnt. Descripción P. Unit Importe
                1 PRODUCTO CUALQUIERA 9,99
                TOTAL (€) 9,99
                IVA BASE IMP. (€) CUOTA (€) TOTAL (€)
                21% 8,26 1,73 9,99
                """;

        assertThat(parser.supports(otro)).isFalse();
    }

    @Test
    @DisplayName("mencionar Mercadona no basta: un extracto bancario no es un ticket")
    void merelyMentioningMercadonaIsNotEnough() {
        // Este es el caso que el reconocimiento anterior dejaba pasar: buscaba la palabra
        // "MERCADONA" en cualquier parte del texto.
        String extracto = """
                BANCO EJEMPLO - EXTRACTO DE CUENTA
                FECHA        CONCEPTO                      IMPORTE
                12/09/2026   COMPRA MERCADONA                 3,77
                26/09/2026   COMPRA MERCADONA, S.A.          73,70
                TOTAL (€) 77,47
                """;

        assertThat(parser.supports(extracto)).isFalse();
    }

    @Test
    @DisplayName("sin la sección de artículos no se reconoce, aunque el emisor sea Mercadona")
    void mercadonaWithoutItemsSectionIsNotRecognised() {
        String sinArticulos = """
                MERCADONA, S.A.   A-46103834
                CL EJEMPLO, 1
                26/09/2026 14:04
                TOTAL (€) 73,70
                """;

        assertThat(parser.supports(sinArticulos)).isFalse();
    }

    @Test
    @DisplayName("sin línea de total no se reconoce, aunque el emisor sea Mercadona")
    void mercadonaWithoutTotalIsNotRecognised() {
        String sinTotal = """
                MERCADONA, S.A.   A-46103834
                CL EJEMPLO, 1
                26/09/2026 14:04  OP: 1000001
                FACTURA SIMPLIFICADA: 2345-012-184052
                Cnt. Descripción P. Unit Importe
                1 ACEITE GIRASOL 1,85
                """;

        assertThat(parser.supports(sinTotal)).isFalse();
    }

    @Test
    @DisplayName("se reconoce por CIF aunque el nombre legal venga partido")
    void recognisedByTaxIdAlone() {
        String porCif = """
                MERCADONA
                A-46103834
                CL EJEMPLO, 1
                26/09/2026 14:04  OP: 1000001
                FACTURA SIMPLIFICADA: 2345-012-184052
                Cnt. Descripción P. Unit Importe
                1 ACEITE GIRASOL 1,85
                TOTAL (€) 1,85
                """;

        assertThat(parser.supports(porCif)).isTrue();
    }

    @Test
    @DisplayName("una linea irreconocible deja el ticket en REVIEW, no tumba el parseo")
    void unknownLineLeavesReceiptInReview() {
        String text = """
                MERCADONA, S.A.   A-46103834
                CL EJEMPLO, 1
                00000 CIUDAD
                TELÉFONO: 000000000
                26/09/2026 14:04  OP: 1000001
                FACTURA SIMPLIFICADA: 2345-012-184052
                Cnt. Descripción P. Unit Importe
                1 ACEITE GIRASOL 1,85
                ~~ linea corrupta ~~
                TOTAL (€) 1,85
                TARJETA BANCARIA 1,85
                """;

        ParsedReceipt receipt = parser.parse(text);

        assertThat(receipt.status()).isEqualTo(ParsingStatus.REVIEW);
        assertThat(receipt.items()).hasSize(1);
        assertThat(receipt.totalAmount()).isEqualByComparingTo(new BigDecimal("1.85"));
        assertThat(receipt.warnings()).anySatisfy(
                warning -> assertThat(warning).contains("linea corrupta"));
    }

    @Test
    @DisplayName("si la suma de lineas no cuadra con el total, el ticket queda en REVIEW")
    void mismatchedTotalLeavesReceiptInReview() {
        String text = """
                MERCADONA, S.A.   A-46103834
                CL EJEMPLO, 1
                00000 CIUDAD
                26/09/2026 14:04  OP: 1000001
                FACTURA SIMPLIFICADA: 2345-012-184052
                Cnt. Descripción P. Unit Importe
                1 ACEITE GIRASOL 1,85
                TOTAL (€) 99,00
                TARJETA BANCARIA 99,00
                """;

        ParsedReceipt receipt = parser.parse(text);

        assertThat(receipt.status()).isEqualTo(ParsingStatus.REVIEW);
        assertThat(receipt.warnings()).anySatisfy(
                warning -> assertThat(warning).contains("no cuadra con el total"));
    }

    @Test
    @DisplayName("sin fecha ni total el ticket es ERROR")
    void missingEssentialsIsError() {
        ParsedReceipt receipt = parser.parse("MERCADONA, S.A.   A-46103834\nalgo ilegible\n");

        assertThat(receipt.status()).isEqualTo(ParsingStatus.ERROR);
        assertThat(receipt.purchasedAt()).isNull();
        assertThat(receipt.totalAmount()).isNull();
    }

    @Test
    @DisplayName("importes en formato español, con coma decimal")
    void parsesSpanishDecimalComma() {
        ParsedReceipt receipt = parser.parse(TicketFixtures.textOf(TicketFixtures.TICKET_73_70));

        // 7,50 no puede acabar convertido en 750 ni en 7.5 mal escalado.
        ParsedReceiptItem nesquik = itemNamed(receipt, "NESQUIK 1K");
        assertThat(nesquik.lineTotal()).isEqualByComparingTo(new BigDecimal("7.50"));
        assertThat(nesquik.lineTotal().scale()).isEqualTo(2);
    }

    private static ParsedReceiptItem itemNamed(ParsedReceipt receipt, String description) {
        return receipt.items().stream()
                .filter(item -> item.rawDescription().equals(description))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontro la linea: " + description));
    }
}
