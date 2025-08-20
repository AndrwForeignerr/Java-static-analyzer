package testing;

import java.io.*;
import java.sql.*;
import java.util.*;

public class TestAnalyzer {
    
    // Security Issue: Hardcoded credentials
    private String password = "admin123";
    private static final String API_KEY = "sk-1234567890abcdef";
    private String username = "administrator";
    
    // Optimization Issue: Unused variables
    private String unusedField = "This is never used";
    private int unusedCounter = 0;
    
    // Security Issue: Weak random number generator
    private Random random = new Random();
    
    // Optimization Issue: Too many parameters
    public void methodWithTooManyParams(String param1, int param2, double param3, 
                                       boolean param4, String param5, Object param6, 
                                       List<String> param7, Map<String, Object> param8) {
        System.out.println("Too many parameters!");
    }
    
    // Security Issue: SQL Injection vulnerability
    public void vulnerableSQLQuery(String userInput) throws SQLException {
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        
        // Direct string concatenation in SQL query
        String query = "SELECT * FROM users WHERE username = '" + userInput + "'";
        ResultSet rs = stmt.executeQuery(query);
        
        while (rs.next()) {
            System.out.println(rs.getString("username"));
        }
    }
    
    // Security Issue: Command injection
    public void executeCommand(String userCommand) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        runtime.exec("cmd.exe /c " + userCommand);
    }
    
    // Security Issue: Path traversal vulnerability
    public void readFile(String filename) throws IOException {
        File file = new File("/var/data/" + filename);
        FileInputStream fis = new FileInputStream(file);
        // ... read file
        fis.close();
    }
    
    // Optimization Issue: String concatenation in loop
    public String inefficientStringConcat() {
        String result = "";
        for (int i = 0; i < 1000; i++) {
            result = result + "Item " + i + ", ";
        }
        return result;
    }
    
    // Security Issue: Null pointer dereference potential
    public void nullPointerRisk(String input) {
        // No null check before using input
        int length = input.length();
        System.out.println("Length: " + length);
    }
    
    // Optimization Issue: Unnecessary object creation
    public void unnecessaryObjects() {
        String s1 = new String("Hello");
        String s2 = new String();
        Integer num = new Integer(42);
        Boolean bool = new Boolean(true);
    }
    
    // Security Issue: Empty catch block
    public void emptyExceptionHandler() {
        try {
            riskyOperation();
        } catch (Exception e) {
            // Empty catch block - security issue
        }
    }
    
    // Security Issue: Sensitive data exposure
    public void sensitiveDataInString() {
        String creditCardNumber = "1234-5678-9012-3456";
        String socialSecurityNumber = "123-45-6789";
        String privateKey = "-----BEGIN RSA PRIVATE KEY-----";
    }
    
    // Optimization Issue: Loop invariant calculation
    public void inefficientLoop() {
        for (int i = 0; i < 1000; i++) {
            double expensiveCalc = Math.sqrt(Math.pow(10, 3));
            System.out.println(i * expensiveCalc);
        }
    }
    
    // Security Issue: Unsafe casting
    public void unsafeCast(Object obj) {
        String str = (String) obj;  // No instanceof check
        System.out.println(str.toUpperCase());
    }
    
    // Security Issue: Array bounds issue
    public void arrayBoundsRisk(int index) {
        int[] array = new int[10];
        int value = array[index];  // No bounds checking
        System.out.println(value);
    }
    
    // Optimization Issue: Division by power of 2
    public int inefficientDivision(int value) {
        return value / 8;  // Could be optimized to value >> 3
    }
    
    // Complexity Issue: High cyclomatic complexity
    public String complexMethod(int a, int b, String mode) {
        if (a > 0) {
            if (b > 0) {
                if (mode.equals("ADD")) {
                    return String.valueOf(a + b);
                } else if (mode.equals("SUBTRACT")) {
                    return String.valueOf(a - b);
                } else if (mode.equals("MULTIPLY")) {
                    return String.valueOf(a * b);
                } else if (mode.equals("DIVIDE")) {
                    if (b != 0) {
                        return String.valueOf(a / b);
                    } else {
                        return "Error";
                    }
                }
            } else {
                if (mode.equals("SPECIAL")) {
                    return "Special case";
                }
            }
        } else {
            if (b < 0) {
                return "Both negative";
            }
        }
        return "Unknown";
    }
    
    // Helper methods
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost/test", "root", password);
    }
    
    private void riskyOperation() throws Exception {
        throw new Exception("Something went wrong");
    }
    
    // Optimization Issue: Empty method
    public void emptyMethod() {
        // This method does nothing
    }
    
    // Unused private method
    private void unusedMethod() {
        System.out.println("This method is never called");
    }
}