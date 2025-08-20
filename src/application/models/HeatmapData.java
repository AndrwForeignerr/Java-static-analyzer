package application.models;

import java.util.*;

public class HeatmapData {
    
    private List<ClassMetrics> classMetrics = new ArrayList<>();
    private List<MethodMetrics> methodMetrics = new ArrayList<>();
    private Map<Integer, List<CodeIssue>> issueDistribution = new HashMap<>();
    private Map<Integer, Integer> lineComplexity = new HashMap<>();
    private Map<Integer, Double> lineQualityScores = new HashMap<>();
    private Map<String, Double> methodQualityScores = new HashMap<>();
    private Map<String, Double> classQualityScores = new HashMap<>();
    private List<HeatmapRegion> heatmapRegions = new ArrayList<>();
    
    public void addClassMetrics(ClassMetrics metrics) {
        classMetrics.add(metrics);
    }
    
    public void addMethodMetrics(MethodMetrics metrics) {
        methodMetrics.add(metrics);
    }
    
    public void incrementLineComplexity(int lineNumber) {
        lineComplexity.put(lineNumber, lineComplexity.getOrDefault(lineNumber, 0) + 1);
    }
    
    public int getLineComplexity(int lineNumber) {
        return lineComplexity.getOrDefault(lineNumber, 0);
    }
    
    public List<ClassMetrics> getClassMetrics() { return classMetrics; }
    public List<MethodMetrics> getMethodMetrics() { return methodMetrics; }
    public Map<Integer, List<CodeIssue>> getIssueDistribution() { return issueDistribution; }
    public Map<Integer, Double> getLineQualityScores() { return lineQualityScores; }
    public Map<String, Double> getMethodQualityScores() { return methodQualityScores; }
    public Map<String, Double> getClassQualityScores() { return classQualityScores; }
    public List<HeatmapRegion> getHeatmapRegions() { return heatmapRegions; }
    
    public void setIssueDistribution(Map<Integer, List<CodeIssue>> issueDistribution) {
        this.issueDistribution = issueDistribution;
    }
    
    public void setLineQualityScores(Map<Integer, Double> lineQualityScores) {
        this.lineQualityScores = lineQualityScores;
    }
    
    public void setMethodQualityScores(Map<String, Double> methodQualityScores) {
        this.methodQualityScores = methodQualityScores;
    }
    
    public void setClassQualityScores(Map<String, Double> classQualityScores) {
        this.classQualityScores = classQualityScores;
    }
    
    public void setHeatmapRegions(List<HeatmapRegion> heatmapRegions) {
        this.heatmapRegions = heatmapRegions;
    }
    
    public double getOverallQualityScore() {
        if (lineQualityScores.isEmpty()) return 100.0;
        
        return lineQualityScores.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(100.0);
    }
    
    public int getTotalIssueCount() {
        return issueDistribution.values().stream()
            .mapToInt(List::size)
            .sum();
    }
    
    public int getCriticalIssueCount() {
        return issueDistribution.values().stream()
            .flatMap(List::stream)
            .mapToInt(issue -> issue.getSeverity() == IssueSeverity.CRITICAL ? 1 : 0)
            .sum();
    }
}






