import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DBConnectionExample {
    public static void main(String[] args) {
        // Database connection details
        String url = "jdbc:mysql://localhost:3306/testdb"; // Change DB name
        String user = "root";  // Change username
        String password = "password"; // Change password

        // Load MySQL JDBC Driver
        DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());

        // Establish the connection
        Connection conn = DriverManager.getConnection(url, user, password);

        // Create statement
        Statement stmt = conn.createStatement();

        // Run a simple query
        stmt.execute("SELECT 1");

        System.out.println("✅ Database connection established successfully!");

        // Close connection
        stmt.close();
        conn.close();
    }
}
