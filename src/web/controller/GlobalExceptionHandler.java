package web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for debugging
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Map<String, Object> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", java.time.Instant.now().toString());
        error.put("status", 500);
        error.put("error", "Internal Server Error");
        error.put("message", ex.getMessage());
        error.put("path", request.getRequestURI());
        error.put("exception", ex.getClass().getName());
        return error;
    }
}