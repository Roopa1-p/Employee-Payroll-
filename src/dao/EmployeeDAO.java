package dao;

import db.DBConnection;
import model.Employee;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    public void addEmployee(Employee employee) throws SQLException {
        String sql = "INSERT INTO employee(name, designation, basic_salary, hra, da, deductions) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, employee.getName());
            ps.setString(2, employee.getDesignation());
            ps.setBigDecimal(3, nullToZero(employee.getBasicSalary()));
            ps.setBigDecimal(4, nullToZero(employee.getHra()));
            ps.setBigDecimal(5, nullToZero(employee.getDa()));
            ps.setBigDecimal(6, nullToZero(employee.getDeductions()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    employee.setId(rs.getInt(1));
                }
            }
        }
    }

    public boolean updateEmployee(Employee employee) throws SQLException {
        String sql = "UPDATE employee SET name=?, designation=?, basic_salary=?, hra=?, da=?, deductions=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, employee.getName());
            ps.setString(2, employee.getDesignation());
            ps.setBigDecimal(3, nullToZero(employee.getBasicSalary()));
            ps.setBigDecimal(4, nullToZero(employee.getHra()));
            ps.setBigDecimal(5, nullToZero(employee.getDa()));
            ps.setBigDecimal(6, nullToZero(employee.getDeductions()));
            ps.setInt(7, employee.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteEmployee(int id) throws SQLException {
        String sql = "DELETE FROM employee WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public Employee getEmployeeById(int id) throws SQLException {
        String sql = "SELECT id, name, designation, basic_salary, hra, da, deductions FROM employee WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Employee> getAllEmployees() throws SQLException {
        String sql = "SELECT id, name, designation, basic_salary, hra, da, deductions FROM employee ORDER BY id";
        List<Employee> employees = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                employees.add(mapRow(rs));
            }
        }
        return employees;
    }

    private Employee mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String designation = rs.getString("designation");
        BigDecimal basic = rs.getBigDecimal("basic_salary");
        BigDecimal hra = rs.getBigDecimal("hra");
        BigDecimal da = rs.getBigDecimal("da");
        BigDecimal deductions = rs.getBigDecimal("deductions");
        return new Employee(id, name, designation, basic, hra, da, deductions);
    }

    private BigDecimal nullToZero(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }
}
