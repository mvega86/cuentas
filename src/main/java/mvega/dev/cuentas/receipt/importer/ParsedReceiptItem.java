package mvega.dev.cuentas.receipt.importer;

import java.math.BigDecimal;
import mvega.dev.cuentas.receipt.ItemUnit;

/**
 * Una linea de compra tal y como la entendio el parser, antes de persistirse.
 *
 * <p>En articulos al peso, {@code quantity} es el peso medido y {@code unit} es
 * {@link ItemUnit#KG}, de modo que {@code quantity * pricePerKg} reproduce
 * {@code lineTotal}. En articulos por pieza, {@code quantity} es el numero de
 * unidades y {@code unitPrice} el precio unitario cuando el ticket lo imprime.
 */
public record ParsedReceiptItem(
        int lineNumber,
        String rawDescription,
        BigDecimal quantity,
        ItemUnit unit,
        BigDecimal unitPrice,
        BigDecimal weightKg,
        BigDecimal pricePerKg,
        BigDecimal lineTotal) {}
