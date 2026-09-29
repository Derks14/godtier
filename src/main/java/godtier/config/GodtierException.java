package godtier.config;

import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public class GodtierException extends RuntimeException {
    private final String code;
    private final HttpStatus httpStatus;

    public GodtierException(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public GodtierException(String code, HttpStatus httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public GodtierException(String code, HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }


}
