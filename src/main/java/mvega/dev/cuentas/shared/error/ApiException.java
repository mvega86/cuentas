package mvega.dev.cuentas.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio que lleva su propio codigo HTTP.
 *
 * <p>A diferencia de la version anterior, el mensaje se delega a
 * {@link RuntimeException} en lugar de guardarse en un campo que tapaba
 * {@code Throwable.getMessage()}.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
