package com.chronos.usercrud;

import java.sql.*;
import java.util.List;
import java.util.Scanner;

/**
 * UserCrudCLI - Command-line interface for user management.
 * Provides simple CRUD operations without JavaFX dependency.
 */
public class UserCrudCLI {
    private final UserDAO userDAO = new UserDAO();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new UserCrudCLI().run();
    }

    public void run() {
        System.out.println("=== Chronos User Manager (CLI) ===");
        System.out.println("Connected to PostgreSQL at localhost:5432/chronos");
        System.out.println();

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Select option: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> listUsers();
                case "2" -> createUser();
                case "3" -> updateUserRole();
                case "4" -> deleteUser();
                case "5" -> running = false;
                default -> System.out.println("Invalid option. Please try again.");
            }
            System.out.println();
        }

        System.out.println("Goodbye!");
        scanner.close();
    }

    private void printMenu() {
        System.out.println("1. List all users");
        System.out.println("2. Create new user");
        System.out.println("3. Update user role");
        System.out.println("4. Delete user");
        System.out.println("5. Exit");
    }

    private void listUsers() {
        List<User> users = userDAO.getAllUsers();
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        System.out.println("\n--- Users ---");
        System.out.printf("%-5s %-20s %-25s %-12s %s%n", "ID", "Name", "Email", "Role", "Created At");
        System.out.println("----------------------------------------------------------------------------------");
        for (User user : users) {
            System.out.printf("%-5d %-20s %-25s %-12s %s%n",
                user.getId(),
                truncate(user.getName(), 20),
                truncate(user.getEmail(), 25),
                user.getRole(),
                user.getCreatedAt().toString().replace('T', ' '));
        }
    }

    private void createUser() {
        System.out.print("Enter name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Enter email: ");
        String email = scanner.nextLine().trim();

        if (name.isEmpty() || email.isEmpty()) {
            System.out.println("Name and email are required.");
            return;
        }

        if (userDAO.userExists(email)) {
            System.out.println("User with this email already exists.");
            return;
        }

        System.out.println("Select role:");
        System.out.println("1. ADMIN");
        System.out.println("2. MANAGER");
        System.out.println("3. DEVELOPER");
        System.out.print("Choice: ");

        String roleChoice = scanner.nextLine().trim();
        User.Role role = switch (roleChoice) {
            case "1" -> User.Role.ADMIN;
            case "2" -> User.Role.MANAGER;
            case "3" -> User.Role.DEVELOPER;
            default -> User.Role.DEVELOPER;
        };

        User user = new User(name, email, role);
        if (userDAO.createUser(user)) {
            System.out.println("User created successfully: " + name + " (" + role + ")");
        } else {
            System.out.println("Failed to create user.");
        }
    }

    private void updateUserRole() {
        System.out.print("Enter user ID: ");
        String idStr = scanner.nextLine().trim();

        try {
            long id = Long.parseLong(idStr);
            User user = userDAO.getUserById(id);

            if (user == null) {
                System.out.println("User not found.");
                return;
            }

            System.out.println("Current user: " + user.getName() + " (" + user.getRole() + ")");
            System.out.println("Select new role:");
            System.out.println("1. ADMIN");
            System.out.println("2. MANAGER");
            System.out.println("3. DEVELOPER");
            System.out.print("Choice: ");

            String roleChoice = scanner.nextLine().trim();
            User.Role newRole = switch (roleChoice) {
                case "1" -> User.Role.ADMIN;
                case "2" -> User.Role.MANAGER;
                case "3" -> User.Role.DEVELOPER;
                default -> User.Role.DEVELOPER;
            };

            user.setRole(newRole);
            user.setUpdatedAt(java.time.LocalDateTime.now());

            if (userDAO.updateUser(user)) {
                System.out.println("User role updated to: " + newRole);
            } else {
                System.out.println("Failed to update user.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid user ID.");
        }
    }

    private void deleteUser() {
        System.out.print("Enter user ID to delete: ");
        String idStr = scanner.nextLine().trim();

        try {
            long id = Long.parseLong(idStr);
            User user = userDAO.getUserById(id);

            if (user == null) {
                System.out.println("User not found.");
                return;
            }

            System.out.print("Are you sure you want to delete " + user.getName() + "? (y/N): ");
            String confirm = scanner.nextLine().trim().toLowerCase();

            if (confirm.equals("y") || confirm.equals("yes")) {
                if (userDAO.deleteUser(id)) {
                    System.out.println("User deleted: " + user.getName());
                } else {
                    System.out.println("Failed to delete user.");
                }
            } else {
                System.out.println("Delete cancelled.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid user ID.");
        }
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() <= maxLength ? str : str.substring(0, maxLength - 3) + "...";
    }
}
