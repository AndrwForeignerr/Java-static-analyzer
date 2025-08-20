package application.models;

public class MethodMetrics {
    
    private final String methodName;
    private final String className;
    private final int startLine;
    private final int endLine;
    private final int parameterCount;
    private int cyclomaticComplexity = 1;
    
    public MethodMetrics(String methodName, String className, int startLine, int endLine, int parameterCount) {
        this.methodName = methodName;
        this.className = className;
        this.startLine = startLine;
        this.endLine = endLine;
        this.parameterCount = parameterCount;
    }
    
    public String getMethodName() { return methodName; }
    public String getClassName() { return className; }
    public int getStartLine() { return startLine; }
    public int getEndLine() { return endLine; }
    public int getParameterCount() { return parameterCount; }
    public int getCyclomaticComplexity() { return cyclomaticComplexity; }
    
    public void setCyclomaticComplexity(int cyclomaticComplexity) {
        this.cyclomaticComplexity = cyclomaticComplexity;
    }
    
    public int getLineCount() {
        return endLine - startLine + 1;
    }
}