package co.com.mmartinez.api.error;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Formato único de error que devuelve la API.
 */
public record ErrorResponse(String code, String message, List<String> details, String path,
                            LocalDateTime timestamp) {

    public static ErrorResponse of(String code, String message, List<String> details, String path) {
        return new ErrorResponse(code, message, details, path, LocalDateTime.now());
    }
}
