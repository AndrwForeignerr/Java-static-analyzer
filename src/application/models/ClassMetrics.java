package application.models;

import java.util.ArrayList;
import java.util.List;

public class ClassMetrics {
    
    private final String className;
    private final int startLine;
    private final int methodCount;
    private final int fieldCount;
    private List<String> methodNames = new ArrayList<>();
    
    public ClassMetrics(String className, int startLine, int methodCount, int fieldCount) {
        this.className = className;
        this.startLine = startLine;
        this.methodCount = methodCount;
        this.fieldCount = fieldCount;
    }
    
    public String getClassName() { return className; }
    public int getStartLine() { return startLine; }
    public int getMethodCount() { return methodCount; }
    public int getFieldCount() { return fieldCount; }
    public List<String> getMethodNames() { return methodNames; }
    
    public void setMethodNames(List<String> methodNames) {
        this.methodNames = methodNames;
    }
}