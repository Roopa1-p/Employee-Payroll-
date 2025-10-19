package main;

import dao.EmployeeDAO;
import model.Employee;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class PayrollSystem {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    public static void main(String[] args) {
        new PayrollSystem().run();
    }

    private void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            printMenu();
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1":
                        addEmployee(scanner);
                        break;
                    case "2":
                        updateEmployee(scanner);
                        break;
                    case "3":
                        deleteEmployee(scanner);
                        break;
                    case "4":
                        viewAllEmployees();
                        break;
                    case "5":
                        System.out.println("Exiting. Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Unexpected error: " + e.getMessage());
            }
            System.out.println();
        }
    }

    private void printMenu() {
        System.out.println("==== Employee Payroll System ====");
        System.out.println("1. Add Employee");
        System.out.println("2. Update Employee");
        System.out.println("3. Delete Employee");
        System.out.println("4. View All Employees");
        System.out.println("5. Exit");
    }

    private void addEmployee(Scanner scanner) throws SQLException {
        System.out.println("-- Add New Employee --");
        String name = promptString(scanner, "Name");
        String designation = promptString(scanner, "Designation");
        BigDecimal basic = promptBigDecimal(scanner, "Basic Salary");
        BigDecimal hra = promptBigDecimal(scanner, "HRA");
        BigDecimal da = promptBigDecimal(scanner, "DA");
        BigDecimal deductions = promptBigDecimal(scanner, "Deductions");

        Employee employee = new Employee(name, designation, basic, hra, da, deductions);
        employeeDAO.addEmployee(employee);
        System.out.println("Employee added with ID: " + employee.getId());
    }

    private void updateEmployee(Scanner scanner) throws SQLException {
        System.out.println("-- Update Employee --");
        int id = promptInt(scanner, "Employee ID");
        Employee existing = employeeDAO.getEmployeeById(id);
        if (existing == null) {
            System.out.println("Employee not found.");
            return;
        }
        System.out.println("Leave a field blank to keep current value.");
        String name = promptStringAllowBlank(scanner, "Name (current: " + existing.getName() + ")");
        String designation = promptStringAllowBlank(scanner, "Designation (current: " + existing.getDesignation() + ")");
        BigDecimal basic = promptBigDecimalAllowBlank(scanner, "Basic Salary (current: " + existing.getBasicSalary() + ")");
        BigDecimal hra = promptBigDecimalAllowBlank(scanner, "HRA (current: " + existing.getHra() + ")");
        BigDecimal da = promptBigDecimalAllowBlank(scanner, "DA (current: " + existing.getDa() + ")");
        BigDecimal deductions = promptBigDecimalAllowBlank(scanner, "Deductions (current: " + existing.getDeductions() + ")");

        if (!name.isEmpty()) existing.setName(name);
        if (!designation.isEmpty()) existing.setDesignation(designation);
        if (basic != null) existing.setBasicSalary(basic);
        if (hra != null) existing.setHra(hra);
        if (da != null) existing.setDa(da);
        if (deductions != null) existing.setDeductions(deductions);

        boolean updated = employeeDAO.updateEmployee(existing);
        System.out.println(updated ? "Employee updated." : "No changes made.");
    }

    private void deleteEmployee(Scanner scanner) throws SQLException {
        System.out.println("-- Delete Employee --");
        int id = promptInt(scanner, "Employee ID");
        boolean deleted = employeeDAO.deleteEmployee(id);
        System.out.println(deleted ? "Employee deleted." : "Employee not found.");
    }

    private void viewAllEmployees() throws SQLException {
        System.out.println("-- All Employee Payroll Details --");
        List<Employee> employees = employeeDAO.getAllEmployees();
        if (employees.isEmpty()) {
            System.out.println("No employees found.");
            return;
        }

        String format = "%-5s %-20s %-18s %12s %12s %12s %12s %12s %n";
        System.out.printf(format, "ID", "Name", "Designation", "Basic", "HRA", "DA", "Deductions", "Net");
        System.out.println(repeat('-', 109));
        for (Employee e : employees) {
            System.out.printf(format,
                    e.getId(),
                    truncate(e.getName(), 20),
                    truncate(e.getDesignation(), 18),
                    e.getBasicSalary().setScale(2),
                    e.getHra().setScale(2),
                    e.getDa().setScale(2),
                    e.getDeductions().setScale(2),
                    e.getNetSalaryRounded());
        }
    }

    private String promptString(Scanner scanner, String label) {
        while (true) {
            System.out.print(label + ": ");
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.println("Value cannot be empty.");
        }
    }

    private String promptStringAllowBlank(Scanner scanner, String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }

    private int promptInt(Scanner scanner, String label) {
        while (true) {
            System.out.print(label + ": ");
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private BigDecimal promptBigDecimal(Scanner scanner, String label) {
        while (true) {
            System.out.print(label + ": ");
            String input = scanner.nextLine().trim();
            try {
                return new BigDecimal(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private BigDecimal promptBigDecimalAllowBlank(Scanner scanner, String label) {
        while (true) {
            System.out.print(label + ": ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return null;
            try {
                return new BigDecimal(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number or leave blank.");
            }
        }
    }

    private String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }
}
