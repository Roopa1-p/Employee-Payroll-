package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String DEFAULT_URL = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/payroll?useSSL=false&serverTimezone=UTC");
    private static final String DEFAULT_USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String DEFAULT_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // For older MySQL drivers
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException("MySQL JDBC Driver not found. Add mysql-connector-j to classpath.", ex);
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
    }
}
