package mvega.dev.cuentas.receipt.importer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mvega.dev.cuentas.receipt.ItemUnit;
import mvega.dev.cuentas.receipt.ParsingStatus;
import org.springframework.stereotype.Component;

/**
 * Parser de los tickets en PDF de Mercadona.
 *
 * <p>Trabaja por tokens separados por espacios, <strong>nunca por posiciones fijas de
 * columna</strong>: el ancho de la columna de importes cambia de un ticket a otro (se
 * comprobo sobre los 8 PDFs de muestra), asi que cualquier parseo por offsets seria
 * fragil.
 *
 * <p>Formas de linea que reconoce:
 * <pre>
 *   1 ACEITE GIRASOL                        1,85     cantidad + descripcion + importe
 *   2 ARROZ REDONDO                1,15     2,30     ademas con precio unitario
 *   1 PIMIENTO ROJO                                  articulo al peso: importe en la
 *        0,718 kg          2,50 €/kg        1,80     linea siguiente
 * </pre>
 */
@Component
public class MercadonaTicketParser implements ReceiptParser {

    public static final String MERCHANT_NAME = "Mercadona";

    private static final String PARSER_VERSION = "mercadona-1";
    private static final String CURRENCY = "EUR";

    /** Margen admitido al cuadrar la suma de lineas contra el total (CLAUDE §6). */
    private static final BigDecimal TOTAL_TOLERANCE = new BigDecimal("0.02");

    /** Importe en formato español: miles con punto opcional, dos decimales con coma. */
    private static final String AMOUNT = "\\d{1,3}(?:\\.\\d{3})*,\\d{2}";

    private static final Pattern COMPANY = Pattern.compile("^(.*?)\\s+([A-Z]-?\\d{8})$");

    /**
     * El nombre legal del emisor, tal y como lo imprime la cabecera del ticket. No vale la
     * simple aparición de la palabra "Mercadona": un apunte bancario o una factura de otra
     * empresa pueden mencionarla sin ser un ticket.
     */
    private static final Pattern LEGAL_NAME =
            Pattern.compile("MERCADONA\\s*,?\\s*S\\.?\\s*A\\.?", Pattern.CASE_INSENSITIVE);

    /** El CIF del emisor, como segunda vía de identificación. */
    private static final Pattern MERCHANT_TAX_ID = Pattern.compile("\\bA-?46103834\\b");
    private static final Pattern PHONE = Pattern.compile("TEL[EÉ]FONO\\s*:?\\s*(\\d{6,15})");
    private static final Pattern PURCHASED_AT =
            Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s+(\\d{1,2}:\\d{2})");
    private static final Pattern OPERATION = Pattern.compile("OP\\s*:?\\s*(\\d+)");
    private static final Pattern INVOICE =
            Pattern.compile("FACTURA\\s+SIMPLIFICADA\\s*:?\\s*([0-9A-Za-z-]+)");

    /** La linea del total. Los parentesis descartan la fila "TOTAL" del desglose de IVA. */
    private static final Pattern TOTAL_LINE =
            Pattern.compile("TOTAL\\s*\\([^)]*\\)\\s+(" + AMOUNT + ")\\s*$");

    private static final Pattern CARD_LAST4 = Pattern.compile("\\*{2,}\\s*(\\d{4})");
    private static final Pattern ITEMS_HEADER =
            Pattern.compile("Cnt.*(?:Importe|Descripci)", Pattern.CASE_INSENSITIVE);

    private static final Pattern ITEM_WITH_UNIT_PRICE =
            Pattern.compile("^(\\d+)\\s+(.+?)\\s+(" + AMOUNT + ")\\s+(" + AMOUNT + ")$");
    private static final Pattern ITEM_WITH_TOTAL =
            Pattern.compile("^(\\d+)\\s+(.+?)\\s+(" + AMOUNT + ")$");
    private static final Pattern ITEM_WITHOUT_AMOUNT = Pattern.compile("^(\\d+)\\s+(\\S.*)$");

    /** Linea de continuacion de un articulo al peso: empieza por "<peso> kg". */
    private static final Pattern WEIGHT_LINE = Pattern.compile("^(\\d+(?:,\\d+)?)\\s*kg\\b(.*)$");
    private static final Pattern AMOUNT_TOKEN = Pattern.compile(AMOUNT);

    private static final Pattern TAX_ROW = Pattern.compile(
            "^(\\d{1,2})\\s*%\\s+(" + AMOUNT + ")\\s+(" + AMOUNT + ")\\s+(" + AMOUNT + ")$");

    private static final Pattern PAYMENT_LINE =
            Pattern.compile("^([\\p{L}.\\s]+?)\\s+(" + AMOUNT + ")$");

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("es-ES"));

    @Override
    public String merchantName() {
        return MERCHANT_NAME;
    }

    @Override
    public String parserVersion() {
        return PARSER_VERSION;
    }

    /**
     * Comprueba que el documento es de verdad un ticket de Mercadona <strong>antes</strong> de
     * interpretar nada.
     *
     * <p>No basta con que aparezca la palabra "Mercadona": un extracto bancario con la línea
     * {@code COMPRA MERCADONA 23,45} la contiene, y no es un ticket. Tampoco basta con que
     * haya palabras genéricas como TOTAL, FECHA, IVA o importes, porque cualquier factura las
     * tiene.
     *
     * <p>Se exigen tres cosas a la vez:
     * <ol>
     *   <li>el emisor, por nombre legal ({@code MERCADONA, S.A.}) o por CIF;</li>
     *   <li>el arranque de la sección de artículos: la cabecera de columnas o, en su defecto,
     *       la factura simplificada, que es de donde parte el parser cuando no hay cabecera;</li>
     *   <li>la línea del total.</li>
     * </ol>
     *
     * <p>Los dos últimos son exactamente lo que el parser necesita para acotar y leer las
     * líneas. Si no están, no puede interpretar el documento y no debe decir que lo soporta:
     * mejor un {@code UNSUPPORTED} claro que un ticket inventado.
     */
    @Override
    public boolean supports(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return false;
        }
        List<String> lines = normalizeLines(rawText);

        boolean issuedByMercadona = indexOfMatch(lines, LEGAL_NAME) >= 0
                || indexOfMatch(lines, MERCHANT_TAX_ID) >= 0;
        if (!issuedByMercadona) {
            return false;
        }

        boolean itemsSectionStarts = indexOfMatch(lines, ITEMS_HEADER) >= 0
                || indexOfMatch(lines, INVOICE) >= 0;
        boolean hasTotalLine = indexOfMatch(lines, TOTAL_LINE) >= 0;

        return itemsSectionStarts && hasTotalLine;
    }

    @Override
    public ParsedReceipt parse(String rawText) {
        List<String> lines = normalizeLines(rawText);
        List<String> warnings = new ArrayList<>();

        String taxId = null;
        int companyIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            Matcher matcher = COMPANY.matcher(lines.get(i));
            if (matcher.matches()) {
                taxId = matcher.group(2);
                companyIndex = i;
                break;
            }
        }

        LocalDateTime purchasedAt = findPurchasedAt(lines, warnings);
        String operationNumber = firstGroup(lines, OPERATION);
        String invoiceNumber = firstGroup(lines, INVOICE);
        String storePhone = firstGroup(lines, PHONE);
        String storeAddress = extractStoreAddress(lines, companyIndex);

        int totalIndex = indexOfMatch(lines, TOTAL_LINE);
        BigDecimal totalAmount = null;
        if (totalIndex >= 0) {
            Matcher matcher = TOTAL_LINE.matcher(lines.get(totalIndex));
            if (matcher.find()) {
                totalAmount = amount(matcher.group(1));
            }
        } else {
            warnings.add("No se encontro la linea del total del ticket");
        }

        List<ParsedReceiptItem> items = extractItems(lines, totalIndex, warnings);
        List<ParsedTaxLine> taxLines = extractTaxLines(lines, totalIndex);
        String paymentMethod = extractPaymentMethod(lines, totalIndex);
        String cardLast4 = firstGroup(lines, CARD_LAST4);

        checkTotals(items, taxLines, totalAmount, warnings);

        ParsingStatus status = resolveStatus(purchasedAt, totalAmount, items, warnings);

        return new ParsedReceipt(MERCHANT_NAME, taxId, null, storeAddress, storePhone,
                purchasedAt, operationNumber, invoiceNumber, CURRENCY, totalAmount,
                paymentMethod, cardLast4, items, taxLines, List.copyOf(warnings), status);
    }

    // --- Cabecera ---

    private LocalDateTime findPurchasedAt(List<String> lines, List<String> warnings) {
        for (String line : lines) {
            Matcher matcher = PURCHASED_AT.matcher(line);
            if (matcher.find()) {
                try {
                    LocalDate date = LocalDate.parse(matcher.group(1), DATE_FORMAT);
                    String[] time = matcher.group(2).split(":");
                    return LocalDateTime.of(date,
                            LocalTime.of(Integer.parseInt(time[0]), Integer.parseInt(time[1])));
                } catch (DateTimeParseException | NumberFormatException e) {
                    warnings.add("Fecha u hora ilegible: " + matcher.group(0));
                    return null;
                }
            }
        }
        warnings.add("No se encontro la fecha del ticket");
        return null;
    }

    /**
     * Junta las lineas de direccion que van entre el nombre de la empresa y el telefono
     * o la fecha. En los tickets de muestra son la calle y el codigo postal con la ciudad.
     */
    private String extractStoreAddress(List<String> lines, int companyIndex) {
        if (companyIndex < 0) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (int i = companyIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (PHONE.matcher(line).find() || PURCHASED_AT.matcher(line).find()
                    || INVOICE.matcher(line).find() || ITEMS_HEADER.matcher(line).find()) {
                break;
            }
            parts.add(line);
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    // --- Lineas de producto ---

    private List<ParsedReceiptItem> extractItems(List<String> lines, int totalIndex,
            List<String> warnings) {
        int start = indexOfMatch(lines, ITEMS_HEADER);
        if (start < 0) {
            // Sin cabecera de columnas, arrancamos tras la factura simplificada.
            start = indexOfMatch(lines, INVOICE);
        }
        int end = totalIndex >= 0 ? totalIndex : lines.size();
        if (start < 0 || start >= end) {
            warnings.add("No se localizo la seccion de articulos");
            return List.of();
        }

        List<ItemDraft> drafts = new ArrayList<>();
        for (int i = start + 1; i < end; i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }

            Matcher weight = WEIGHT_LINE.matcher(line);
            if (weight.matches()) {
                applyWeight(drafts, weight, line, warnings);
                continue;
            }

            Matcher withUnitPrice = ITEM_WITH_UNIT_PRICE.matcher(line);
            if (withUnitPrice.matches()) {
                drafts.add(ItemDraft.byUnits(drafts.size() + 1, withUnitPrice.group(2).trim(),
                        new BigDecimal(withUnitPrice.group(1)),
                        amount(withUnitPrice.group(3)), amount(withUnitPrice.group(4))));
                continue;
            }

            Matcher withTotal = ITEM_WITH_TOTAL.matcher(line);
            if (withTotal.matches()) {
                drafts.add(ItemDraft.byUnits(drafts.size() + 1, withTotal.group(2).trim(),
                        new BigDecimal(withTotal.group(1)), null, amount(withTotal.group(3))));
                continue;
            }

            Matcher withoutAmount = ITEM_WITHOUT_AMOUNT.matcher(line);
            if (withoutAmount.matches()) {
                // Cabecera de un articulo al peso: el importe llega en la linea siguiente.
                drafts.add(ItemDraft.pendingWeight(drafts.size() + 1,
                        withoutAmount.group(2).trim(), new BigDecimal(withoutAmount.group(1))));
                continue;
            }

            warnings.add("Linea de articulo no reconocida: " + line);
        }

        List<ParsedReceiptItem> items = new ArrayList<>(drafts.size());
        for (ItemDraft draft : drafts) {
            if (draft.lineTotal == null) {
                // Se conserva la linea para que el usuario la repase, en lugar de
                // descartarla y falsear el ticket (CLAUDE §6).
                warnings.add("Articulo sin importe: " + draft.rawDescription);
                draft.lineTotal = BigDecimal.ZERO;
            }
            items.add(draft.toRecord());
        }
        return items;
    }

    /** Completa el articulo anterior con el peso, el precio por kilo y el importe. */
    private void applyWeight(List<ItemDraft> drafts, Matcher weight, String line,
            List<String> warnings) {
        if (drafts.isEmpty()) {
            warnings.add("Linea de peso sin articulo al que asociarla: " + line);
            return;
        }
        List<BigDecimal> amounts = new ArrayList<>();
        Matcher tokens = AMOUNT_TOKEN.matcher(weight.group(2));
        while (tokens.find()) {
            amounts.add(amount(tokens.group()));
        }
        if (amounts.size() < 2) {
            warnings.add("Linea de peso sin precio por kilo e importe: " + line);
            return;
        }
        ItemDraft draft = drafts.get(drafts.size() - 1);
        BigDecimal weightKg = decimal(weight.group(1));
        draft.unit = ItemUnit.KG;
        draft.quantity = weightKg;
        draft.weightKg = weightKg;
        draft.pricePerKg = amounts.get(amounts.size() - 2);
        draft.lineTotal = amounts.get(amounts.size() - 1);
        draft.unitPrice = null;
    }

    // --- Pie del ticket ---

    private List<ParsedTaxLine> extractTaxLines(List<String> lines, int totalIndex) {
        List<ParsedTaxLine> taxLines = new ArrayList<>();
        int start = Math.max(totalIndex, 0);
        for (int i = start; i < lines.size(); i++) {
            Matcher matcher = TAX_ROW.matcher(lines.get(i).trim());
            if (matcher.matches()) {
                taxLines.add(new ParsedTaxLine(new BigDecimal(matcher.group(1)),
                        amount(matcher.group(2)), amount(matcher.group(3)),
                        amount(matcher.group(4))));
            }
        }
        return taxLines;
    }

    /** La forma de pago es la primera linea con texto e importe despues del total. */
    private String extractPaymentMethod(List<String> lines, int totalIndex) {
        if (totalIndex < 0) {
            return null;
        }
        for (int i = totalIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (TAX_ROW.matcher(line).matches() || line.startsWith("IVA")) {
                break;
            }
            Matcher matcher = PAYMENT_LINE.matcher(line);
            if (matcher.matches()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    // --- Cuadres y estado ---

    private void checkTotals(List<ParsedReceiptItem> items, List<ParsedTaxLine> taxLines,
            BigDecimal totalAmount, List<String> warnings) {
        if (totalAmount == null) {
            return;
        }
        BigDecimal itemsSum = items.stream()
                .map(ParsedReceiptItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (itemsSum.subtract(totalAmount).abs().compareTo(TOTAL_TOLERANCE) > 0) {
            warnings.add("La suma de las lineas (%s) no cuadra con el total del ticket (%s)"
                    .formatted(itemsSum.toPlainString(), totalAmount.toPlainString()));
        }
        if (!taxLines.isEmpty()) {
            BigDecimal taxSum = taxLines.stream()
                    .map(ParsedTaxLine::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (taxSum.subtract(totalAmount).abs().compareTo(TOTAL_TOLERANCE) > 0) {
                warnings.add("El desglose de IVA (%s) no cuadra con el total (%s)"
                        .formatted(taxSum.toPlainString(), totalAmount.toPlainString()));
            }
        }
    }

    private ParsingStatus resolveStatus(LocalDateTime purchasedAt, BigDecimal totalAmount,
            List<ParsedReceiptItem> items, List<String> warnings) {
        if (purchasedAt == null || totalAmount == null || items.isEmpty()) {
            return ParsingStatus.ERROR;
        }
        return warnings.isEmpty() ? ParsingStatus.PARSED : ParsingStatus.REVIEW;
    }

    // --- Utilidades ---

    private static List<String> normalizeLines(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (String line : rawText.split("\\R")) {
            // El espacio duro rompe los \s+ de los patrones si no se normaliza.
            lines.add(line.replace(' ', ' ').stripTrailing());
        }
        return lines;
    }

    private static int indexOfMatch(List<String> lines, Pattern pattern) {
        for (int i = 0; i < lines.size(); i++) {
            if (pattern.matcher(lines.get(i)).find()) {
                return i;
            }
        }
        return -1;
    }

    private static String firstGroup(List<String> lines, Pattern pattern) {
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /** Importe español a BigDecimal: quita el punto de miles y cambia la coma por punto. */
    private static BigDecimal amount(String raw) {
        return new BigDecimal(raw.replace(".", "").replace(',', '.'));
    }

    /** Decimal sin separador de miles, como el peso en kilos. */
    private static BigDecimal decimal(String raw) {
        return new BigDecimal(raw.replace(',', '.'));
    }

    /** Linea en construccion: los articulos al peso se completan en dos pasos. */
    private static final class ItemDraft {
        private int lineNumber;
        private String rawDescription;
        private BigDecimal quantity;
        private ItemUnit unit;
        private BigDecimal unitPrice;
        private BigDecimal weightKg;
        private BigDecimal pricePerKg;
        private BigDecimal lineTotal;

        static ItemDraft byUnits(int lineNumber, String description, BigDecimal quantity,
                BigDecimal unitPrice, BigDecimal lineTotal) {
            ItemDraft draft = new ItemDraft();
            draft.lineNumber = lineNumber;
            draft.rawDescription = description;
            draft.quantity = quantity;
            draft.unit = ItemUnit.UNIT;
            draft.unitPrice = unitPrice;
            draft.lineTotal = lineTotal;
            return draft;
        }

        static ItemDraft pendingWeight(int lineNumber, String description, BigDecimal quantity) {
            ItemDraft draft = new ItemDraft();
            draft.lineNumber = lineNumber;
            draft.rawDescription = description;
            draft.quantity = quantity;
            draft.unit = ItemUnit.UNIT;
            return draft;
        }

        ParsedReceiptItem toRecord() {
            return new ParsedReceiptItem(lineNumber, rawDescription, quantity, unit, unitPrice,
                    weightKg, pricePerKg, lineTotal);
        }
    }
}
