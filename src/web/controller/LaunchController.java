package web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LaunchController {

    /**
     * Attempts to launch the JavaFX desktop GUI from the web interface.
     * In a Docker/WSL2 environment with WSLg display support, this starts
     * the JavaFX application as a separate process.
     */
    @PostMapping("/launch-javafx")
    public ResponseEntity<Map<String, Object>> launchJavaFX() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Check if we're in an environment with display support
            String display = System.getenv("DISPLAY");
            String waylandDisplay = System.getenv("WAYLAND_DISPLAY");

            if (display == null && waylandDisplay == null) {
                result.put("success", false);
                result.put("message", "No display server detected. JavaFX GUI requires DISPLAY or WAYLAND_DISPLAY environment variable. Start the app with WSLg or X11 forwarding enabled.");
                return ResponseEntity.ok(result);
            }

            // Attempt to launch JavaFX Main class as a separate process
            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "--module-path", "/usr/share/javafx/lib",
                    "--add-modules", "javafx.controls,javafx.graphics,javafx.base,javafx.fxml",
                    "-cp", "target/chronos-1.0.0-SNAPSHOT.jar:target/classes",
                    "Main"
            );
            pb.redirectErrorStream(true);
            pb.start();

            result.put("success", true);
            result.put("message", "JavaFX GUI launched successfully on display: " + (display != null ? display : waylandDisplay));
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Failed to launch JavaFX GUI: " + e.getMessage() + ". Ensure Java 21 and JavaFX 21 are installed, and DISPLAY/WAYLAND_DISPLAY is set.");
            return ResponseEntity.ok(result);
        }
    }
}
