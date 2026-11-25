package cz.example.shopassistant.common.error;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Převádí výjimky na jednotný chybový formát popsaný v {@code docs/api.md}.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrorBody> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        HttpStatus resolved = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        String message = exception.getReason() != null ? exception.getReason() : resolved.getReasonPhrase();
        return build(resolved, codeFor(resolved), message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorBody> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Neplatný vstup.");
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorBody> handleUnreadable(HttpMessageNotReadableException exception) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Neplatný formát požadavku.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiErrorBody> handleTooLarge(MaxUploadSizeExceededException exception) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "Soubor je příliš velký.");
    }

    @ExceptionHandler({ NoResourceFoundException.class, NoHandlerFoundException.class })
    ResponseEntity<ApiErrorBody> handleNotFound(Exception exception) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Zdroj neexistuje.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorBody> handleUnexpected(Exception exception) {
        LOG.error("Neočekávaná chyba", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Došlo k neočekávané chybě. Zkuste to prosím znovu.");
    }

    private static ResponseEntity<ApiErrorBody> build(HttpStatus status, String code, String message) {
        ApiErrorBody body = new ApiErrorBody(
                Instant.now(),
                status.value(),
                code,
                message,
                UUID.randomUUID().toString().substring(0, 8));
        return ResponseEntity.status(status).body(body);
    }

    private static String codeFor(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "VALIDATION_ERROR";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case NOT_FOUND -> "NOT_FOUND";
            case CONFLICT -> "CONFLICT";
            case PAYLOAD_TOO_LARGE -> "PAYLOAD_TOO_LARGE";
            case TOO_MANY_REQUESTS -> "RATE_LIMITED";
            default -> "INTERNAL_ERROR";
        };
    }
}
