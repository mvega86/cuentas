package mvega.dev.cuentas.shared.text;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normaliza descripciones de producto para poder compararlas entre tickets.
 *
 * <p>Mercadona imprime las descripciones en mayusculas y abreviadas, pero no siempre
 * con los mismos espacios ni acentos. La normalizacion es deterministica y sin IA:
 * primero reglas, y la correccion a mano encima (CLAUDE §11).
 */
public final class ProductNameNormalizer {

    private ProductNameNormalizer() {
    }

    /**
     * Pasa a mayusculas, quita acentos y colapsa espacios.
     *
     * <p>{@code "  Yog. Nat Azucar Caña "} da {@code "YOG. NAT AZUCAR CANA"}.
     *
     * @return el texto normalizado, o {@code null} si la entrada era nula o vacia
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String withoutAccents = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return withoutAccents.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
