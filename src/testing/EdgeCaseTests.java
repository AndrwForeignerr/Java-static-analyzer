package testing;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class EdgeCaseTests {
    
    public void testExceptionHandling() {
        try {
            riskyOperation();
        } catch (Exception e) {
            // Should detect: empty catch block
        }
        
        try {
            anotherRiskyOperation();
        } catch (IOException e) {
            e.printStackTrace();  // Good: proper handling
        }
    }
    
    public void testEnhancedForLoopVariable() {
        List<String> items = Arrays.asList("a", "b", "c");
        
        for (String item : items) {  
            // 'item' should NOT be flagged as potential null
            System.out.println(item.length());
            System.out.println(item.toUpperCase());
        }
    }
    
    public void testFieldVsLocalVariable() {
        String localVar = "test";  // If unused, should be flagged
        this.fieldVar = "field";   // Should NOT be flagged as unused
    }
    
    private String fieldVar;
    
    public void testMethodChaining() {
        // Should not flag null pointer for method chaining on known objects
        String result = "hello".toUpperCase().substring(0, 3);
        System.out.println(result);
    }
    
    private void riskyOperation() throws Exception {
        throw new Exception("Test");
    }
    
    private void anotherRiskyOperation() throws IOException {
        throw new IOException("Test");
    }
}