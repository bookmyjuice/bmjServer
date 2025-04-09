package online.bmj.www.Exceptions;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
//import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import online.bmj.www.DTOs.chargeBeeErrorResponse;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ChargebeeException.class)
    public ResponseEntity<chargeBeeErrorResponse> handleChargebeeExceptions(ChargebeeException ex) {
        chargeBeeErrorResponse error = new chargeBeeErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            Map.of("timestamp", Instant.now())
        );
        
        if(ex instanceof RateLimitExceededException rateEx) {
            error.addDetail("retry_after_seconds", rateEx.getRetryAfterSeconds());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
        }
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public chargeBeeErrorResponse handleGenericExceptions(Exception ex) {
        return new chargeBeeErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            Map.of("timestamp", Instant.now())
        );
    }
}