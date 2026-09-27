package mvega.dev.cuentas.shared.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Estructura unica de error de la API (CLAUDE §9). Antes se devolvia el mensaje
 * pelado, sin forma estable que el frontend pudiera tratar.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldIssue> fieldErrors) {

    /** Un problema concreto de validacion sobre un campo de entrada. */
    public record FieldIssue(String field, String message) {}

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, List.of());
    }
}
