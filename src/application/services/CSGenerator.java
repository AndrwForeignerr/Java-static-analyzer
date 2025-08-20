package application.services;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CSGenerator {
    
    public String generateOptimizationSuggestion(String issueType, Node problemNode, MethodDeclaration containingMethod) {
        CodeContext context = extractContext(problemNode, containingMethod);
        
        switch (issueType) {
            case "UNUSED_VARIABLE":
                return generateUnusedVariableSuggestion(context);
            case "STRING_CONCATENATION_IN_LOOP":
                return generateStringConcatenationSuggestion(context);
            case "LOOP_INVARIANT_CALCULATION":
                return generateLoopInvariantSuggestion(context);
            case "UNNECESSARY_OBJECT_CREATION":
                return generateObjectCreationSuggestion(context);
            case "WRAPPER_OBJECT_CREATION":
                return generateWrapperObjectSuggestion(context);
            case "UNINITIALIZED_VARIABLE":
                return generateInitializationSuggestion(context);
            case "TOO_MANY_PARAMETERS":
                return generateParameterReductionSuggestion(context);
            case "DIVISION_OPTIMIZATION":
                return generateDivisionOptimizationSuggestion(context);
            default:
                return generateGenericOptimizationSuggestion(context);
        }
    }
    
    public String generateSecuritySuggestion(String issueType, Node problemNode, MethodDeclaration containingMethod) {
        CodeContext context = extractContext(problemNode, containingMethod);
        
        switch (issueType) {
            case "SQL_INJECTION":
                return generateSQLInjectionFixSuggestion(context);
            case "COMMAND_INJECTION":
                return generateCommandInjectionFixSuggestion(context);
            case "PATH_TRAVERSAL":
                return generatePathTraversalFixSuggestion(context);
            case "NULL_POINTER_DEREFERENCE":
                return generateNullPointerFixSuggestion(context);
            case "UNSAFE_CASTING":
                return generateUnsafeCastingFixSuggestion(context);
            case "WEAK_RANDOM":
                return generateWeakRandomFixSuggestion(context);
            case "SENSITIVE_DATA_EXPOSURE":
                return generateSensitiveDataFixSuggestion(context);
            case "ARRAY_BOUNDS_CHECK":
                return generateArrayBoundsFixSuggestion(context);
            case "EMPTY_CATCH_BLOCK":
                return generateEmptyCatchFixSuggestion(context);
            default:
                return generateGenericSecuritySuggestion(context);
        }
    }
    
    private CodeContext extractContext(Node problemNode, MethodDeclaration containingMethod) {
        CodeContext context = new CodeContext();
        context.problemNode = problemNode;
        context.containingMethod = containingMethod;
        context.originalCode = problemNode.toString();
        
        if (problemNode instanceof VariableDeclarator) {
            VariableDeclarator vd = (VariableDeclarator) problemNode;
            context.variableName = vd.getNameAsString();
            context.variableType = vd.getTypeAsString();
            context.hasInitializer = vd.getInitializer().isPresent();
            if (context.hasInitializer) {
                context.initializer = vd.getInitializer().get().toString();
            }
        }
        
        if (problemNode instanceof BinaryExpr) {
            BinaryExpr be = (BinaryExpr) problemNode;
            context.binaryOperator = be.getOperator().toString();
            context.leftOperand = be.getLeft().toString();
            context.rightOperand = be.getRight().toString();
            
            if (be.getLeft() instanceof NameExpr) {
                context.variableName = ((NameExpr) be.getLeft()).getNameAsString();
            }
        }
        
        if (problemNode instanceof MethodCallExpr) {
            MethodCallExpr mce = (MethodCallExpr) problemNode;
            context.methodName = mce.getNameAsString();
            context.methodArguments = new ArrayList<>();
            mce.getArguments().forEach(arg -> context.methodArguments.add(arg.toString()));
            
            if (mce.getScope().isPresent()) {
                context.methodScope = mce.getScope().get().toString();
            }
        }
        
        if (problemNode instanceof ObjectCreationExpr) {
            ObjectCreationExpr oce = (ObjectCreationExpr) problemNode;
            context.objectType = oce.getTypeAsString();
            context.constructorArguments = new ArrayList<>();
            oce.getArguments().forEach(arg -> context.constructorArguments.add(arg.toString()));
        }
        
        if (problemNode instanceof CastExpr) {
            CastExpr ce = (CastExpr) problemNode;
            context.castTargetType = ce.getTypeAsString();
            context.castExpression = ce.getExpression().toString();
        }
        
        context.isInLoop = isInsideLoop(problemNode);
        context.loopType = determineLoopType(problemNode);
        context.surroundingVariables = extractSurroundingVariables(containingMethod);
        
        return context;
    }
    
    private String generateUnusedVariableSuggestion(CodeContext context) {
        return String.format(
            "// Remove unused variable:\n" +
            "// %s %s%s;\n\n" +
            "// The variable '%s' is declared but never used.\n" +
            "// Simply delete this line to clean up the code.",
            context.variableType,
            context.variableName,
            context.hasInitializer ? " = " + context.initializer : "",
            context.variableName
        );
    }
    
    private String generateStringConcatenationSuggestion(CodeContext context) {
        if (context.variableName != null) {
            return String.format(
                "// Replace string concatenation with StringBuilder:\n" +
                "StringBuilder %sSb = new StringBuilder(%s);\n" +
                "%sSb.append(%s);\n" +
                "%s = %sSb.toString();\n\n" +
                "// This avoids creating multiple intermediate String objects in the loop",
                context.variableName,
                context.variableName,
                context.variableName,
                context.rightOperand,
                context.variableName,
                context.variableName
            );
        } else {
            return String.format(
                "// Use StringBuilder for efficient concatenation:\n" +
                "StringBuilder sb = new StringBuilder();\n" +
                "sb.append(%s).append(%s);\n" +
                "String result = sb.toString();",
                context.leftOperand,
                context.rightOperand
            );
        }
    }
    
    private String generateLoopInvariantSuggestion(CodeContext context) {
        String calculationVar = "calculated" + capitalize(context.methodName);
        return String.format(
            "// Move expensive calculation outside the loop:\n" +
            "double %s = %s;\n\n" +
            "// Inside the loop, use the pre-calculated value:\n" +
            "// ... use %s instead of %s",
            calculationVar,
            context.originalCode,
            calculationVar,
            context.originalCode
        );
    }
    
    private String generateObjectCreationSuggestion(CodeContext context) {
        if ("String".equals(context.objectType)) {
            return "// Replace unnecessary String creation:\n" +
                   "String variable = \"\"; // Instead of new String()";
        } else {
            return String.format(
                "// Avoid unnecessary object creation:\n" +
                "// Consider reusing existing %s instances or using object pooling",
                context.objectType
            );
        }
    }
    
    private String generateWrapperObjectSuggestion(CodeContext context) {
        String arg = context.constructorArguments.isEmpty() ? "value" : context.constructorArguments.get(0);
        return String.format(
            "// Use valueOf() method for better performance:\n" +
            "%s variable = %s.valueOf(%s);\n\n" +
            "// valueOf() reuses cached instances for common values",
            context.objectType,
            context.objectType,
            arg
        );
    }
    
    private String generateInitializationSuggestion(CodeContext context) {
        String initValue = getDefaultInitialization(context.variableType);
        return String.format(
            "// Initialize variable at declaration:\n" +
            "%s %s = %s;\n\n" +
            "// This prevents potential NullPointerException",
            context.variableType,
            context.variableName,
            initValue
        );
    }
    
    private String generateParameterReductionSuggestion(CodeContext context) {
        String methodName = context.containingMethod.getNameAsString();
        return String.format(
            "// Consider reducing parameters using parameter object pattern:\n" +
            "public class %sParams {\n" +
            "    // Group related parameters into fields\n" +
            "}\n\n" +
            "public void %s(%sParams params) {\n" +
            "    // Use params.field instead of individual parameters\n" +
            "}",
            capitalize(methodName),
            methodName,
            capitalize(methodName)
        );
    }
    
    private String generateDivisionOptimizationSuggestion(CodeContext context) {
        if (context.rightOperand != null) {
            try {
                int divisor = Integer.parseInt(context.rightOperand);
                if (isPowerOfTwo(divisor)) {
                    int shiftAmount = Integer.numberOfTrailingZeros(divisor);
                    return String.format(
                        "// Optimize division by power of 2 using bit shifting:\n" +
                        "%s >> %d // Instead of %s / %d\n\n" +
                        "// Bit shifting is faster than division",
                        context.leftOperand,
                        shiftAmount,
                        context.leftOperand,
                        divisor
                    );
                }
            } catch (NumberFormatException e) {
                // Fallback
            }
        }
        return "// Consider optimizing division operation";
    }
    
    private String generateSQLInjectionFixSuggestion(CodeContext context) {
        if (context.methodArguments != null && !context.methodArguments.isEmpty()) {
            String query = context.methodArguments.get(0);
            return String.format(
                "// Fix SQL injection vulnerability:\n" +
                "PreparedStatement stmt = conn.prepareStatement(\n" +
                "    \"SELECT * FROM users WHERE id = ?\");\n" +
                "stmt.setString(1, userId);\n" +
                "ResultSet rs = stmt.executeQuery();\n\n" +
                "// Never concatenate user input directly into SQL queries"
            );
        }
        return "// Use PreparedStatement with parameterized queries to prevent SQL injection";
    }
    
    private String generateCommandInjectionFixSuggestion(CodeContext context) {
        return String.format(
            "// Fix command injection vulnerability:\n" +
            "ProcessBuilder pb = new ProcessBuilder(\"command\", validatedInput);\n" +
            "Process process = pb.start();\n\n" +
            "// OR validate and sanitize input:\n" +
            "if (input.matches(\"[a-zA-Z0-9]+\")) {\n" +
            "    Runtime.getRuntime().exec(\"command \" + input);\n" +
            "}\n\n" +
            "// Never execute commands with unsanitized user input"
        );
    }
    
    private String generatePathTraversalFixSuggestion(CodeContext context) {
        return String.format(
            "// Fix path traversal vulnerability:\n" +
            "Path safePath = Paths.get(\"/safe/directory\").resolve(filename).normalize();\n" +
            "if (!safePath.startsWith(\"/safe/directory\")) {\n" +
            "    throw new SecurityException(\"Invalid path\");\n" +
            "}\n" +
            "FileInputStream fis = new FileInputStream(safePath.toFile());\n\n" +
            "// Always validate and normalize file paths"
        );
    }
    
    private String generateNullPointerFixSuggestion(CodeContext context) {
        return String.format(
            "// Add null check before method call:\n" +
            "if (%s != null) {\n" +
            "    %s;\n" +
            "}\n\n" +
            "// OR use Optional for better null handling:\n" +
            "Optional.ofNullable(%s).ifPresent(obj -> obj.someMethod());",
            extractVariableFromMethodCall(context.originalCode),
            context.originalCode,
            extractVariableFromMethodCall(context.originalCode)
        );
    }
    
    private String generateUnsafeCastingFixSuggestion(CodeContext context) {
        return String.format(
            "// Add instanceof check before casting:\n" +
            "if (obj instanceof %s) {\n" +
            "    %s variable = (%s) obj;\n" +
            "    // Use variable safely here\n" +
            "}\n\n" +
            "// This prevents ClassCastException",
            context.castTargetType,
            context.castTargetType,
            context.castTargetType
        );
    }
    
    private String generateWeakRandomFixSuggestion(CodeContext context) {
        return "// Use SecureRandom for cryptographic purposes:\n" +
               "SecureRandom secureRandom = new SecureRandom();\n" +
               "int secureValue = secureRandom.nextInt(1000000);\n\n" +
               "// SecureRandom provides cryptographically strong random numbers";
    }
    
    private String generateSensitiveDataFixSuggestion(CodeContext context) {
        return String.format(
            "// Store sensitive data securely:\n" +
            "char[] %s = {'s', 'e', 'c', 'r', 'e', 't'};\n" +
            "// Process the data...\n" +
            "Arrays.fill(%s, '\\0'); // Clear from memory\n\n" +
            "// char[] can be cleared, String cannot",
            context.variableName != null ? context.variableName : "sensitiveData",
            context.variableName != null ? context.variableName : "sensitiveData"
        );
    }
    
    private String generateArrayBoundsFixSuggestion(CodeContext context) {
        return "// Add bounds checking before array access:\n" +
               "if (index >= 0 && index < array.length) {\n" +
               "    int value = array[index];\n" +
               "    // Use value safely\n" +
               "}\n\n" +
               "// This prevents ArrayIndexOutOfBoundsException";
    }
    
    private String generateEmptyCatchFixSuggestion(CodeContext context) {
        return "// Handle exceptions properly:\n" +
               "catch (Exception e) {\n" +
               "    logger.error(\"Error occurred: \", e);\n" +
               "    // Add appropriate error handling\n" +
               "}\n\n" +
               "// Empty catch blocks hide important errors";
    }
    
    private String generateGenericOptimizationSuggestion(CodeContext context) {
        return "// Consider optimizing this code section:\n" +
               "// " + context.originalCode + "\n\n" +
               "// Review for performance improvements";
    }
    
    private String generateGenericSecuritySuggestion(CodeContext context) {
        return "// Review this code for security issues:\n" +
               "// " + context.originalCode + "\n\n" +
               "// Ensure proper input validation and error handling";
    }
    
    private String getDefaultInitialization(String type) {
        if (type.contains("List")) return "new ArrayList<>()";
        if (type.contains("Map")) return "new HashMap<>()";
        if (type.contains("Set")) return "new HashSet<>()";
        if (type.equals("String")) return "\"\"";
        if (type.equals("StringBuilder")) return "new StringBuilder()";
        if (isPrimitiveType(type)) return "0";
        return "null";
    }
    
    private boolean isPrimitiveType(String type) {
        return type.equals("int") || type.equals("long") || type.equals("double") || 
               type.equals("float") || type.equals("boolean") || type.equals("char") || 
               type.equals("byte") || type.equals("short");
    }
    
    private boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
    
    private boolean isInsideLoop(Node node) {
        Node parent = node.getParentNode().orElse(null);
        while (parent != null) {
            if (parent instanceof ForStmt || parent instanceof WhileStmt || 
                parent instanceof DoStmt || parent instanceof ForEachStmt) {
                return true;
            }
            parent = parent.getParentNode().orElse(null);
        }
        return false;
    }
    
    private String determineLoopType(Node node) {
        Node parent = node.getParentNode().orElse(null);
        while (parent != null) {
            if (parent instanceof ForStmt) return "FOR";
            if (parent instanceof WhileStmt) return "WHILE";
            if (parent instanceof DoStmt) return "DO_WHILE";
            if (parent instanceof ForEachStmt) return "FOR_EACH";
            parent = parent.getParentNode().orElse(null);
        }
        return "NONE";
    }
    
    private List<String> extractSurroundingVariables(MethodDeclaration method) {
        List<String> variables = new ArrayList<>();
        if (method != null) {
            method.accept(new com.github.javaparser.ast.visitor.VoidVisitorAdapter<Void>() {
                @Override
                public void visit(VariableDeclarator vd, Void arg) {
                    variables.add(vd.getNameAsString());
                    super.visit(vd, arg);
                }
            }, null);
        }
        return variables;
    }
    
    private String extractVariableFromMethodCall(String methodCall) {
        if (methodCall.contains(".")) {
            return methodCall.substring(0, methodCall.indexOf("."));
        }
        return "variable";
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private static class CodeContext {
        Node problemNode;
        MethodDeclaration containingMethod;
        String originalCode;
        String variableName;
        String variableType;
        boolean hasInitializer;
        String initializer;
        String binaryOperator;
        String leftOperand;
        String rightOperand;
        String methodName;
        String methodScope;
        List<String> methodArguments;
        String objectType;
        List<String> constructorArguments;
        String castTargetType;
        String castExpression;
        boolean isInLoop;
        String loopType;
        List<String> surroundingVariables;
    }
}