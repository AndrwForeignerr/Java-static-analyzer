package testing;



import java.io.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.Random;

/**
 * User service with intentional vulnerabilities for static analysis demonstration
 * This represents real-world code patterns that often contain security issues
 */
public class VulnerableUserService {
    
    // HARDCODED CREDENTIALS - Should trigger CRITICAL security issue
    private static final String DB_PASSWORD = "MySecretPassword123!";
    private static final String API_KEY = "sk-1234567890abcdefghijklmnopqrstuvwxyz";
    private String adminPassword = "admin123";
    
    private Connection connection;
    private Random randomGenerator = new Random(); // WEAK RANDOM in security context
    
    /**
     * Constructor with database setup
     */
    public VulnerableUserService() {
        try {
            // More hardcoded credentials
            String dbUrl = "jdbc:mysql://localhost:3306/users?user=root&password=" + DB_PASSWORD;
            connection = DriverManager.getConnection(dbUrl);
        } catch (SQLException e) {
            e.printStackTrace(); // POOR EXCEPTION HANDLING
        }
    }
    
    /**
     * SQL INJECTION vulnerability - builds query with string concatenation
     */
    public User findUserById(String userId) {
        User result = null;
        String query = "SELECT * FROM users WHERE id = '" + userId + "'"; // SQL INJECTION!
        
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            if (rs.next()) {
                result = new User();
                result.setId(rs.getString("id"));
                result.setName(rs.getString("name"));
                result.setEmail(rs.getString("email"));
            }
        } catch (SQLException e) {
            // EMPTY CATCH BLOCK - hides errors
        }
        
        return result;
    }
    
    /**
     * Command injection vulnerability
     */
    public String executeSystemCommand(String userCommand) {
        String output = "";
        try {
            // COMMAND INJECTION - dangerous Runtime.exec with user input
            Process process = Runtime.getRuntime().exec("ping " + userCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String line;
            while ((line = reader.readLine()) != null) {
                output += line + "\n"; // STRING CONCATENATION IN LOOP - performance issue
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return output;
    }
    
    /**
     * Path traversal vulnerability
     */
    public String readUserFile(String fileName) {
        String content = "";
        try {
            // PATH TRAVERSAL - user input directly in file path
            FileInputStream fis = new FileInputStream("/app/userfiles/" + fileName);
            Scanner scanner = new Scanner(fis);
            
            while (scanner.hasNextLine()) {
                content += scanner.nextLine() + "\n"; // More string concatenation in loop
            }
            
            scanner.close();
            fis.close();
        } catch (FileNotFoundException e) {
            return "File not found";
        } catch (IOException e) {
            // Another empty catch block
        }
        
        return content;
    }
    
    /**
     * Multiple optimization and security issues
     */
    public List<String> processUserData(List<Object> rawData, String category, int maxItems, 
                                       boolean includeMetadata, String outputFormat, 
                                       Date startDate, Date endDate) { // TOO MANY PARAMETERS
        
        List<String> results = new ArrayList<>();
        String report = ""; // UNINITIALIZED variable that gets concatenated
        
        // INEFFICIENT LOOP - calls size() every iteration
        for (int i = 0; i < rawData.size(); i++) {
            Object item = rawData.get(i);
            
            // UNSAFE CASTING without instanceof check
            String data = (String) item;
            
            if (data != null) {
                // EXPENSIVE CALCULATION IN LOOP
                double processedValue = Math.pow(data.length(), 2) + Math.sqrt(i);
                
                if (processedValue > 10) {
                    results.add(data.toUpperCase());
                    report += "Processed: " + data + "\n"; // STRING CONCATENATION IN LOOP
                }
            }
        }
        
        return results;
    }
    
    /**
     * Null pointer and array bounds issues
     */
    public String getUserInfo(User user, int[] preferences, int preferenceIndex) {
        // NULL POINTER DEREFERENCE - no null check
        String userName = user.getName();
        
        // ARRAY BOUNDS violation - no bounds checking
        int selectedPreference = preferences[preferenceIndex];
        
        return userName + " has preference: " + selectedPreference;
    }
    
    /**
     * Sensitive data exposure
     */
    public boolean authenticateUser(String username, String password) {
        // SENSITIVE DATA in String (should use char[])
        String storedPassword = getUserPassword(username);
        
        // Weak comparison
        return password.equals(storedPassword);
    }
    
    /**
     * Weak cryptography
     */
    public String generateSessionToken() {
        // WEAK RANDOM for security token
        int tokenValue = randomGenerator.nextInt(1000000);
        return "SESSION_" + tokenValue;
    }
    
    /**
     * Method with high cyclomatic complexity
     */
    public String validateAndProcessUser(User user, String operation) {
        if (user == null) {
            return "Invalid user";
        }
        
        if (user.getAge() < 18) {
            if (operation.equals("delete")) {
                return "Cannot delete minor";
            } else if (operation.equals("update")) {
                if (user.hasParentalConsent()) {
                    return "Update allowed with consent";
                } else {
                    return "Update requires parental consent";
                }
            }
        } else {
            if (operation.equals("create")) {
                if (user.getEmail() != null && user.getEmail().contains("@")) {
                    if (user.getName() != null && user.getName().length() > 2) {
                        return "User created successfully";
                    } else {
                        return "Invalid name";
                    }
                } else {
                    return "Invalid email";
                }
            } else if (operation.equals("update")) {
                if (user.isActive()) {
                    return "User updated";
                } else {
                    return "Cannot update inactive user";
                }
            } else if (operation.equals("delete")) {
                if (user.hasActiveSubscription()) {
                    return "Cannot delete user with active subscription";
                } else {
                    return "User deleted";
                }
            }
        }
        
        return "Unknown operation";
    }
    
    // UNUSED VARIABLE in method
    public void processPayment(double amount) {
        double tax = amount * 0.1; // Declared but never used
        String currency = "USD";   // Also unused
        
        System.out.println("Processing payment: $" + amount);
    }
    
    /**
     * Unnecessary object creation
     */
    public String formatData() {
        String result = new String(); // UNNECESSARY - should be ""
        Boolean flag = new Boolean(true); // Should use Boolean.valueOf(true)
        Integer count = new Integer(42); // Should use Integer.valueOf(42)
        
        return result + flag + count;
    }
    
    /**
     * Division optimization opportunity
     */
    public int[] optimizeArray(int[] data) {
        int[] result = new int[data.length];
        
        for (int i = 0; i < data.length; i++) {
            result[i] = data[i] / 8; // DIVISION BY POWER OF 2 - could use >> 3
        }
        
        return result;
    }
    
    // Helper method for realistic context
    private String getUserPassword(String username) {
        // In real code, this would query database
        return "hashedPassword123";
    }
    
    // Empty method
    public void logActivity() {
        // EMPTY METHOD BODY
    }
    
    // Nested class for completeness
    public static class User {
        private String id;
        private String name;
        private String email;
        private int age;
        private boolean active;
        private boolean hasParentalConsent;
        private boolean hasActiveSubscription;
        
        // Standard getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        
        public boolean hasParentalConsent() { return hasParentalConsent; }
        public void setHasParentalConsent(boolean hasParentalConsent) { 
            this.hasParentalConsent = hasParentalConsent; 
        }
        
        public boolean hasActiveSubscription() { return hasActiveSubscription; }
        public void setHasActiveSubscription(boolean hasActiveSubscription) { 
            this.hasActiveSubscription = hasActiveSubscription; 
        }
    }
}