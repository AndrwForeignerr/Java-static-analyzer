package testing;

import java.sql.*;
import java.io.*;
import java.util.Random;

public class SecurityTestVulnerable {
    
    // Should detect: sensitive data in String
    private String password = "secret123";
    private String apiKey = "api_key_value";
    
    public void testSQLInjection(String userId) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/db");
            Statement stmt = conn.createStatement();
            // Should detect: SQL injection vulnerability
            String query = "SELECT * FROM users WHERE id = '" + userId + "'";
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                System.out.println(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void testCommandInjection(String userInput) {
        try {
            // Should detect: command injection
            Runtime.getRuntime().exec("ls -la " + userInput);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void testFilePathTraversal(String fileName) {
        try {
            // Should detect: path traversal
            FileInputStream fis = new FileInputStream("/uploads/" + fileName);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void testWeakRandom() {
        // Should detect: weak random in security context
        Random random = new Random();
        String token = "TOKEN_" + random.nextInt(100000);
        String sessionKey = "KEY_" + random.nextLong();
    }
    
    public void testUnsafeCasting(Object obj) {
        // Should detect: unsafe casting without instanceof
        String str = (String) obj;
        Integer num = (Integer) obj;
    }
    
    public void testArrayBoundsIssue(int[] array, int userIndex) {
        // Should detect: array access without bounds check
        int value = array[userIndex];
        System.out.println(value);
    }
    
    public void testNullPointerIssues() {
        String nullString = null;
        // Should detect: potential null pointer
        int length = nullString.length();
        
        Object obj = getSomeObject();
        // Should detect: potential null pointer  
        String result = obj.toString();
    }
    
    private Object getSomeObject() {
        return Math.random() > 0.5 ? "hello" : null;
    }
}
