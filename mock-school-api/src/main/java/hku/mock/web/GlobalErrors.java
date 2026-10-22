/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.mock.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/** 统一错误体：{error, message}。与项目主服务的错误形状保持一致。 */
@RestControllerAdvice
public class GlobalErrors {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(body(codeOf(e.getStatusCode().value()), e.getReason()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(body("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> other(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body("INTERNAL_ERROR", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
    }

    private static Map<String, Object> body(String error, String message) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("error", error);
        out.put("message", message == null ? "" : message);
        return out;
    }

    private static String codeOf(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 404 -> "NOT_FOUND";
            case 503 -> "SERVICE_UNAVAILABLE";
            default -> "ERROR_" + status;
        };
    }
}
