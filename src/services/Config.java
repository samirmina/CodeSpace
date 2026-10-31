package services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration loader - reads from environment variables, system properties,
 * config.properties file, with fallback to defaults.
 * Priority: env vars > system properties > config.properties > defaults
 */
public class Config {
    private static final String CONFIG_FILE = "config.properties";
    private static final Properties props = new Properties();
    private static boolean loaded = false;

    // Default values
    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/chronos";
    private static final String DEFAULT_DB_USER = "postgres";
    private static final String DEFAULT_DB_PASSWORD = "postgres";
    private static final String DEFAULT_DATA_DIR = ".";
    private static final String DEFAULT_AUTO_SAVE_INTERVAL = "30";

    static {
        load();
    }

    private static void load() {
        if (loaded) return;

        // Load config.properties if it exists
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

        // Environment variables and system properties override file config
        // Check env vars first, then system properties
        setIfPresent("db.url", "DB_URL", DEFAULT_DB_URL);
        setIfPresent("db.user", "DB_USER", DEFAULT_DB_USER);
        setIfPresent("db.password", "DB_PASSWORD", DEFAULT_DB_PASSWORD);
        setIfPresent("app.data.dir", "APP_DATA_DIR", DEFAULT_DATA_DIR);
        setIfPresent("app.auto.save.interval", "APP_AUTO_SAVE_INTERVAL", DEFAULT_AUTO_SAVE_INTERVAL);

        loaded = true;
    }

    private static void setIfPresent(String propKey, String envKey, String defaultValue) {
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isEmpty()) {
            props.setProperty(propKey, envVal);
        } else {
            String sysVal = System.getProperty(propKey);
            if (sysVal != null && !sysVal.isEmpty()) {
                props.setProperty(propKey, sysVal);
            } else if (!props.containsKey(propKey)) {
                props.setProperty(propKey, defaultValue);
            }
        }
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
        return props.getProperty("app.data.dir", DEFAULT_DATA_DIR);
    }

    public static int getAutoSaveInterval() {
        String val = props.getProperty("app.auto.save.interval", DEFAULT_AUTO_SAVE_INTERVAL);
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