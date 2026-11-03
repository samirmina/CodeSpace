package web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import web.renderer.HtmlRenderer;

/**
 * Global exception handler that catches unhandled exceptions and returns
 * properly rendered HTML error pages instead of Spring Boot's Whitelabel Error page.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private final HtmlRenderer renderer;

    public GlobalExceptionHandler(HtmlRenderer renderer) {
        this.renderer = renderer;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        String errorMessage = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred";
        String html = renderer.renderErrorPage(errorMessage, ex.getStackTrace()[0].toString());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.TEXT_HTML)
            .body(html);
    }
}
