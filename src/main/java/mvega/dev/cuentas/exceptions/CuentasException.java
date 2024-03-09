package mvega.dev.cuentas.exceptions;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class CuentasException extends RuntimeException{

    private String message;
    private HttpStatus httpStatus;


    public CuentasException(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
