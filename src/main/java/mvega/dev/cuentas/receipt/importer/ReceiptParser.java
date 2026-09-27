package mvega.dev.cuentas.receipt.importer;

/**
 * Contrato comun de parser de tickets. Existe para que añadir Carrefour, Lidl u otro
 * comercio no obligue a tocar el flujo de importacion (CLAUDE §6).
 *
 * <p>Un parser recibe texto plano, no un PDF: la extraccion de texto esta separada del
 * parseo de negocio, y por eso el mismo parser vale para un PDF subido a mano y para un
 * adjunto bajado de Gmail (CLAUDE §6, §7).
 */
public interface ReceiptParser {

    /** Nombre canonico del comercio, tal y como se guarda en la tabla {@code merchant}. */
    String merchantName();

    /** Version del parser, que se guarda en cada ticket para poder reprocesar despues. */
    String parserVersion();

    /** Indica si este parser reconoce el texto como un ticket suyo. */
    boolean supports(String rawText);

    /** Parsea el texto. No lanza excepcion por una linea rara: la marca en los avisos. */
    ParsedReceipt parse(String rawText);
}
