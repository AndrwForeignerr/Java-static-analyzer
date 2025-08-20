package testing;

import java.util.Arrays;
import java.util.List;

public class OptimizationTestBasic {
    
    public void testUnusedVariables() {
        int unusedVar = 42;  // Should detect: unused variable
        String message = "Hello";
        System.out.println(message);
    }
    
    public void testStringConcatenationInLoop() {
        String result = "";
        for (int i = 0; i < 100; i++) {
            result = result + "Item " + i + "\n";  // Should detect: string concat in loop
        }
        System.out.println(result);
    }
    
    public void testLoopInvariantCalculations() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        for (int num : numbers) {
            double expensive = Math.pow(2.0, 10.0);  // Should detect: loop invariant
            double sqrt = Math.sqrt(144.0);  // Should detect: loop invariant  
            System.out.println(num * expensive * sqrt);
        }
    }
    
    public void testUnnecessaryObjectCreation() {
        String empty = new String();  // Should detect: unnecessary String()
        Integer wrapped = new Integer(42);  // Should detect: use valueOf()
        Boolean bool = new Boolean(true);  // Should detect: use valueOf()
    }
    
    public void testMethodWithTooManyParameters(String a, String b, String c, 
                                               String d, String e, String f, 
                                               String g, String h) {
        // Should detect: too many parameters
        System.out.println("Too many params");
    }
}