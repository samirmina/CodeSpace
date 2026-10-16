package services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration loader - reads from config.properties with fallback to defaults
 */
public class Config {
    private static final String CONFIG_FILE = "config.properties";
    private static final Properties props = new Properties();
    private static boolean loaded = false;

    // Default values
    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/chronos";
    private static final String DEFAULT_DB_USER = "postgres";
    private static final String DEFAULT_DB_PASSWORD = "postgres";

    static {
        load();
    }

    private static void load() {
        if (loaded) return;
        
        try (InputStream in = Files.newInputStream(Paths.get(CONFIG_FILE))) {
            props.load(in);
        } catch (IOException e) {
            // Config file not found, try classpath
            try (InputStream in = Config.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (in != null) {
                    props.load(in);
                }
            } catch (IOException ex) {
                // Use defaults
            }
        }
        loaded = true;
    }

    public static String getDbUrl() {
        return props.getProperty("db.url", DEFAULT_DB_URL);
    }

    public static String getDbUser() {
        return props.getProperty("db.user", DEFAULT_DB_USER);
    }

    public static String getDbPassword() {
        return props.getProperty("db.password", DEFAULT_DB_PASSWORD);
    }

    public static String getDataDir() {
        return props.getProperty("app.data.dir", ".");
    }

    public static int getAutoSaveInterval() {
        String val = props.getProperty("app.auto.save.interval", "30");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    // Reload config (useful for testing or runtime changes)
    public static void reload() {
        loaded = false;
        props.clear();
        load();
    }
}