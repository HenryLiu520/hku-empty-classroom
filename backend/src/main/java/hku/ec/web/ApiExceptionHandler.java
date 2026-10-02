package hku.ec.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Dtos.ApiError> badParam(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest()
                .body(new Dtos.ApiError("BAD_PARAMETER", "Cannot parse parameter: " + e.getName()));
    }

    @ExceptionHandler(java.time.format.DateTimeParseException.class)
    public ResponseEntity<Dtos.ApiError> badDate(java.time.format.DateTimeParseException e) {
        return ResponseEntity.badRequest()
                .body(new Dtos.ApiError("BAD_DATE", "Use yyyy-MM-dd for date and HH:mm for time"));
    }

    /** 时间不符合"整小时"口径，或不满足学校课表优先规则等取值问题 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Dtos.ApiError> badValue(IllegalArgumentException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        String code = msg.startsWith("Timetable has priority") ? "TIMETABLE_PRIORITY" : "BAD_TIME";
        return ResponseEntity.badRequest().body(new Dtos.ApiError(code, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Dtos.ApiError> generic(Exception e) {
        return ResponseEntity.status(500)
                .body(new Dtos.ApiError("SERVER_ERROR", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
    }
}
