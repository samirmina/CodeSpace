package utils;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * FileUtils - provides file operation utilities for reading, writing, and logging
 * Handles all file I/O operations without external dependencies
 */
public class FileUtils {

    // Write content to file - overwrites file if exists or creates new file
    public static void writeToFile(String filename, String content) {
        try {
            Files.writeString(Path.of(filename), content);
            System.out.println("✓ Written to " + filename);
        } catch (IOException e) {
            System.err.println("✗ Error writing file: " + e.getMessage());
        }
    }

    // Read content from file - returns entire file content as string
    public static String readFromFile(String filename) {
        try {
            return Files.readString(Path.of(filename));
        } catch (IOException e) {
            System.err.println("✗ Error reading file: " + e.getMessage());
            return "";
        }
    }

    // Append content to file - adds content to end of existing file
    public static void appendToFile(String filename, String content) {
        try {
            Files.writeString(Path.of(filename), content + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("✗ Error appending file: " + e.getMessage());
        }
    }

    // Log message with timestamp - appends formatted message to log file
    public static void log(String message) {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logEntry = String.format("[%s] %s", timestamp, message);
        appendToFile("app.log", logEntry);
    }

    // Log message with level - appends formatted message with severity level to log
    public static void log_with_level(String level, String message) {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);
        appendToFile("app.log", logEntry);
    }

    // Check if file exists - returns true if file is present
    public static boolean fileExists(String filename) {
        return Files.exists(Path.of(filename));
    }

    // Delete file - removes file from filesystem if it exists
    public static void deleteFile(String filename) {
        try {
            Files.deleteIfExists(Path.of(filename));
            System.out.println("✓ Deleted " + filename);
        } catch (IOException e) {
            System.err.println("✗ Error deleting file: " + e.getMessage());
        }
    }

    // Create directory - creates folder if it doesn't exist
    public static void createDirectory(String dirPath) {
        try {
            Files.createDirectories(Path.of(dirPath));
            System.out.println("✓ Created directory: " + dirPath);
        } catch (IOException e) {
            System.err.println("✗ Error creating directory: " + e.getMessage());
        }
    }

    // Get file size - returns size in bytes
    public static long getFileSize(String filename) {
        try {
            return Files.size(Path.of(filename));
        } catch (IOException e) {
            System.err.println("✗ Error getting file size: " + e.getMessage());
            return 0;
        }
    }

    // Get file extension - returns extension without dot (e.g., "txt")
    public static String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf(".");
        if (lastDot == -1) return "";
        return filename.substring(lastDot + 1);
    }

    // Get file name without extension - returns base filename
    public static String getFileNameWithoutExtension(String filename) {
        int lastDot = filename.lastIndexOf(".");
        if (lastDot == -1) return filename;
        
        int lastSeparator = Math.max(filename.lastIndexOf("/"), filename.lastIndexOf("\\"));
        String name = filename.substring(lastSeparator + 1);
        return name.substring(0, lastDot);
    }

    // Copy file - duplicates file content to new location
    public static void copyFile(String source, String destination) {
        try {
            Files.copy(Path.of(source), Path.of(destination), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✓ Copied " + source + " to " + destination);
        } catch (IOException e) {
            System.err.println("✗ Error copying file: " + e.getMessage());
        }
    }

    // Move file - relocates file to new path
    public static void moveFile(String source, String destination) {
        try {
            Files.move(Path.of(source), Path.of(destination), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✓ Moved " + source + " to " + destination);
        } catch (IOException e) {
            System.err.println("✗ Error moving file: " + e.getMessage());
        }
    }

    // List files in directory - returns array of filenames in folder
    public static String[] listFiles(String directory) {
        try {
            return Files.list(Path.of(directory))
                .map(path -> path.getFileName().toString())
                .toArray(String[]::new);
        } catch (IOException e) {
            System.err.println("✗ Error listing directory: " + e.getMessage());
            return new String[0];
        }
    }

    // Clear file content - empties file while keeping it
    public static void clearFile(String filename) {
        writeToFile(filename, "");
    }
}
