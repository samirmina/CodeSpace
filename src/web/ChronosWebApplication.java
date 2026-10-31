package web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"web", "services", "models", "utils"})
public class ChronosWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChronosWebApplication.class, args);
    }
}
