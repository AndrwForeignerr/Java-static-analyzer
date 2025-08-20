package testing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ComplexTestCase {
    
    private List<Product> products = new ArrayList<>();
    private Map<String, Integer> cache = new HashMap<>();
    
    public void processComplexData(String input) {
        // Mix of good and bad practices
        Objects.requireNonNull(input);  // Good
        
        String result = "";  // Bad: will be used in loop
        List<String> items = parseInput(input);
        
        for (String item : items) {  // 'item' should NOT be flagged as null
            if (item != null) {  // Good: null check
                result = result + item + "\n";  // Bad: string concat in loop
                
                double calculation = Math.sin(Math.PI / 4);  // Bad: loop invariant
                System.out.println(item + ": " + calculation);
            }
        }
        
        // Potential null pointer without check
        Product product = findProduct(input);
        String name = product.getName();  // Should flag if no null check
    }
    
    private List<String> parseInput(String input) {
        return Arrays.asList(input.split(","));
    }
    
    private Product findProduct(String name) {
        return products.stream()
                      .filter(p -> p.getName().equals(name))
                      .findFirst()
                      .orElse(null);  // Returns null if not found
    }
    
    // Inner class for testing
    static class Product {
        private String name;
        
        public Product(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }}