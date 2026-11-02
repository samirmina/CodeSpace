package web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ChronosWebApplication - Spring Boot entry point for the web service.
 * All HTML is generated programmatically by HtmlRenderer - no external HTML/CSS files.
 */
@SpringBootApplication(scanBasePackages = {"web.controller", "web.renderer", "services", "models"})
public class ChronosWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChronosWebApplication.class, args);
    }
}