package application.models;

import java.util.List;
import java.util.Map;

public class ClassAnalysisResult {
    
    private final String decompiledCode;
    private final List<OptimizationSuggestion> optimizationSuggestions;
    private final List<SecurityIssue> securityIssues;
    private final MethodCallGraph methodCallGraph;
    private final ClassHierarchyGraph classHierarchy;
    private final ClassInfo classInfo;
    private final HeatmapData heatmapData;
    
    public ClassAnalysisResult(String decompiledCode,
                             List<OptimizationSuggestion> optimizationSuggestions,
                             List<SecurityIssue> securityIssues,
                             MethodCallGraph methodCallGraph,
                             ClassHierarchyGraph classHierarchy,
                             ClassInfo classInfo,
                             HeatmapData heatmapData) {
        this.decompiledCode = decompiledCode;
        this.optimizationSuggestions = optimizationSuggestions;
        this.securityIssues = securityIssues;
        this.methodCallGraph = methodCallGraph;
        this.classHierarchy = classHierarchy;
        this.classInfo = classInfo;
        this.heatmapData = heatmapData;
    }
    
    public String getDecompiledCode() {
        return decompiledCode;
    }
    
    public List<OptimizationSuggestion> getOptimizationSuggestions() {
        return optimizationSuggestions;
    }
    
    public List<SecurityIssue> getSecurityIssues() {
        return securityIssues;
    }
    
    public MethodCallGraph getMethodCallGraph() {
        return methodCallGraph;
    }
    
    public ClassHierarchyGraph getClassHierarchy() {
        return classHierarchy;
    }
    
    public ClassInfo getClassInfo() {
        return classInfo;
    }
    
    public HeatmapData getHeatmapData() {
        return heatmapData;
    }
}