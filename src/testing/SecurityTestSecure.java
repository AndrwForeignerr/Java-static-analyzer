package testing;

import java.sql.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SecurityTestSecure {
    
    // Good: final static constants for sensitive data
    private static final String DB_URL = System.getProperty("db.url");
    
    private SecureRandom secureRandom = new SecureRandom();  // Good: SecureRandom
    
    public List<String> getUserDataSecure(String userId) {
        Objects.requireNonNull(userId);  // Good: null check
        List<String> results = new ArrayList<>();
        
        try {
            Connection conn = DriverManager.getConnection(DB_URL);
            // Good: PreparedStatement prevents SQL injection
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT name FROM users WHERE id = ? AND active = true");
            stmt.setString(1, userId);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("name"));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
           
        }
        return results;
    }
    
    public void safeCasting(Object obj) {
        // Good: instanceof check before casting
        if (obj instanceof String) {
            String str = (String) obj;
            System.out.println(str);
        }
    }
    
    public void safeArrayAccess(int[] array, int index) {
        // Good: bounds checking
        if (index >= 0 && index < array.length) {
            int value = array[index];
            System.out.println(value);
        }
    }
    
    public String buildReportEfficiently(List<String> items) {
        Objects.requireNonNull(items);
        // Good: StringBuilder for concatenation
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            sb.append(item).append(", ");
        }
        return sb.toString();
    }
}
