package application.services;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import application.models.*;

import java.util.*;

public class HeatmapAnalyzer {
    
    public HeatmapData generateHeatmapData(CompilationUnit compilationUnit, 
                                         List<OptimizationSuggestion> optimizations,
                                         List<SecurityIssue> securityIssues) {
        
        if (compilationUnit == null) {
            return new HeatmapData();
        }
        
        HeatmapData heatmapData = new HeatmapData();
        
        analyzeCodeStructure(compilationUnit, heatmapData);
        calculateComplexityMetrics(compilationUnit, heatmapData);
        mapIssueDistribution(optimizations, securityIssues, heatmapData);
        calculateQualityScores(heatmapData);
        generateHeatmapRegions(heatmapData);
        
        return heatmapData;
    }
    
    private void analyzeCodeStructure(CompilationUnit compilationUnit, HeatmapData heatmapData) {
        StructureAnalyzer analyzer = new StructureAnalyzer(heatmapData);
        compilationUnit.accept(analyzer, null);
    }
    
    private void calculateComplexityMetrics(CompilationUnit compilationUnit, HeatmapData heatmapData) {
        ComplexityCalculator calculator = new ComplexityCalculator(heatmapData);
        compilationUnit.accept(calculator, null);
    }
    
    private void mapIssueDistribution(List<OptimizationSuggestion> optimizations,
                                    List<SecurityIssue> securityIssues, 
                                    HeatmapData heatmapData) {
        
        Map<Integer, List<CodeIssue>> issuesByLine = new HashMap<>();
        
        for (OptimizationSuggestion opt : optimizations) {
            CodeIssue issue = new CodeIssue(
                opt.getLineNumber(),
                IssueType.OPTIMIZATION,
                mapOptimizationSeverity(opt.getSeverity()),
                opt.getDescription()
            );
            issuesByLine.computeIfAbsent(opt.getLineNumber(), k -> new ArrayList<>()).add(issue);
        }
        
        for (SecurityIssue sec : securityIssues) {
            CodeIssue issue = new CodeIssue(
                sec.getLineNumber(),
                IssueType.SECURITY,
                mapSecuritySeverity(sec.getSeverity()),
                sec.getDescription()
            );
            issuesByLine.computeIfAbsent(sec.getLineNumber(), k -> new ArrayList<>()).add(issue);
        }
        
        heatmapData.setIssueDistribution(issuesByLine);
    }
    
    private void calculateQualityScores(HeatmapData heatmapData) {
        Map<Integer, Double> lineQualityScores = new HashMap<>();
        Map<String, Double> methodQualityScores = new HashMap<>();
        Map<String, Double> classQualityScores = new HashMap<>();
        
        for (Map.Entry<Integer, List<CodeIssue>> entry : heatmapData.getIssueDistribution().entrySet()) {
            int lineNumber = entry.getKey();
            List<CodeIssue> issues = entry.getValue();
            
            double qualityScore = calculateLineQualityScore(issues, heatmapData.getLineComplexity(lineNumber));
            lineQualityScores.put(lineNumber, qualityScore);
        }
        
        for (MethodMetrics method : heatmapData.getMethodMetrics()) {
            double methodScore = calculateMethodQualityScore(method, lineQualityScores);
            methodQualityScores.put(method.getMethodName(), methodScore);
        }
        
        for (ClassMetrics clazz : heatmapData.getClassMetrics()) {
            double classScore = calculateClassQualityScore(clazz, methodQualityScores);
            classQualityScores.put(clazz.getClassName(), classScore);
        }
        
        heatmapData.setLineQualityScores(lineQualityScores);
        heatmapData.setMethodQualityScores(methodQualityScores);
        heatmapData.setClassQualityScores(classQualityScores);
    }
    
    private void generateHeatmapRegions(HeatmapData heatmapData) {
        List<HeatmapRegion> regions = new ArrayList<>();
        
        Map<Integer, Double> lineScores = heatmapData.getLineQualityScores();
        List<Integer> sortedLines = new ArrayList<>(lineScores.keySet());
        Collections.sort(sortedLines);
        
        int regionStart = -1;
        double currentRegionScore = 0;
        int regionSize = 0;
        
        for (int i = 0; i < sortedLines.size(); i++) {
            int lineNumber = sortedLines.get(i);
            double lineScore = lineScores.get(lineNumber);
            
            if (regionStart == -1) {
                regionStart = lineNumber;
                currentRegionScore = lineScore;
                regionSize = 1;
            } else if (Math.abs(lineNumber - sortedLines.get(i - 1)) <= 2 && 
                      Math.abs(lineScore - currentRegionScore) <= 20) {
                currentRegionScore = (currentRegionScore * regionSize + lineScore) / (regionSize + 1);
                regionSize++;
            } else {
                if (regionSize >= 3) {
                    regions.add(new HeatmapRegion(
                        regionStart,
                        sortedLines.get(i - 1),
                        currentRegionScore,
                        getHeatIntensity(currentRegionScore),
                        getRegionType(currentRegionScore)
                    ));
                }
                regionStart = lineNumber;
                currentRegionScore = lineScore;
                regionSize = 1;
            }
        }
        
        if (regionSize >= 3) {
            regions.add(new HeatmapRegion(
                regionStart,
                sortedLines.get(sortedLines.size() - 1),
                currentRegionScore,
                getHeatIntensity(currentRegionScore),
                getRegionType(currentRegionScore)
            ));
        }
        
        heatmapData.setHeatmapRegions(regions);
    }
    
    private double calculateLineQualityScore(List<CodeIssue> issues, int complexity) {
        double baseScore = 100.0;
        
        for (CodeIssue issue : issues) {
            switch (issue.getSeverity()) {
                case CRITICAL:
                    baseScore -= 40;
                    break;
                case HIGH:
                    baseScore -= 25;
                    break;
                case MEDIUM:
                    baseScore -= 15;
                    break;
                case LOW:
                    baseScore -= 5;
                    break;
            }
            
            if (issue.getType() == IssueType.SECURITY) {
                baseScore -= 10;
            }
        }
        
        baseScore -= Math.min(complexity * 2, 20);
        
        return Math.max(0, Math.min(100, baseScore));
    }
    
    private double calculateMethodQualityScore(MethodMetrics method, Map<Integer, Double> lineScores) {
        double totalScore = 0;
        int lineCount = 0;
        
        for (int line = method.getStartLine(); line <= method.getEndLine(); line++) {
            if (lineScores.containsKey(line)) {
                totalScore += lineScores.get(line);
                lineCount++;
            }
        }
        
        double averageScore = lineCount > 0 ? totalScore / lineCount : 100;
        
        averageScore -= Math.min(method.getCyclomaticComplexity() * 3, 30);
        averageScore -= Math.min((method.getEndLine() - method.getStartLine()) * 0.5, 20);
        
        return Math.max(0, Math.min(100, averageScore));
    }
    
    private double calculateClassQualityScore(ClassMetrics clazz, Map<String, Double> methodScores) {
        double totalScore = 0;
        int methodCount = 0;
        
        for (String methodName : clazz.getMethodNames()) {
            if (methodScores.containsKey(methodName)) {
                totalScore += methodScores.get(methodName);
                methodCount++;
            }
        }
        
        double averageScore = methodCount > 0 ? totalScore / methodCount : 100;
        
        averageScore -= Math.min(clazz.getFieldCount() * 2, 20);
        averageScore -= Math.min(clazz.getMethodCount() * 1, 15);
        
        return Math.max(0, Math.min(100, averageScore));
    }
    
    private IssueSeverity mapOptimizationSeverity(OptimizationSeverity severity) {
        switch (severity) {
            case CRITICAL: return IssueSeverity.CRITICAL;
            case HIGH: return IssueSeverity.HIGH;
            case MEDIUM: return IssueSeverity.MEDIUM;
            case LOW: return IssueSeverity.LOW;
            default: return IssueSeverity.LOW;
        }
    }
    
    private IssueSeverity mapSecuritySeverity(SecuritySeverity severity) {
        switch (severity) {
            case CRITICAL: return IssueSeverity.CRITICAL;
            case HIGH: return IssueSeverity.HIGH;
            case MEDIUM: return IssueSeverity.MEDIUM;
            case LOW: return IssueSeverity.LOW;
            default: return IssueSeverity.LOW;
        }
    }
    
    private double getHeatIntensity(double qualityScore) {
        if (qualityScore >= 80) return 0.2;
        if (qualityScore >= 60) return 0.4;
        if (qualityScore >= 40) return 0.6;
        if (qualityScore >= 20) return 0.8;
        return 1.0;
    }
    
    private HeatmapRegionType getRegionType(double qualityScore) {
        if (qualityScore >= 80) return HeatmapRegionType.EXCELLENT;
        if (qualityScore >= 60) return HeatmapRegionType.GOOD;
        if (qualityScore >= 40) return HeatmapRegionType.AVERAGE;
        if (qualityScore >= 20) return HeatmapRegionType.POOR;
        return HeatmapRegionType.CRITICAL;
    }
    
    private class StructureAnalyzer extends VoidVisitorAdapter<Void> {
        
        private final HeatmapData heatmapData;
        private String currentClassName;
        private String currentMethodName;
        private int currentMethodStart;
        
        public StructureAnalyzer(HeatmapData heatmapData) {
            this.heatmapData = heatmapData;
        }
        
        @Override
        public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
            currentClassName = cid.getNameAsString();
            
            ClassMetrics classMetrics = new ClassMetrics(
                currentClassName,
                getLineNumber(cid),
                cid.getMethods().size(),
                cid.getFields().size()
            );
            
            List<String> methodNames = new ArrayList<>();
            cid.getMethods().forEach(method -> methodNames.add(method.getNameAsString()));
            classMetrics.setMethodNames(methodNames);
            
            heatmapData.addClassMetrics(classMetrics);
            
            super.visit(cid, arg);
        }
        
        @Override
        public void visit(MethodDeclaration md, Void arg) {
            currentMethodName = md.getNameAsString();
            currentMethodStart = getLineNumber(md);
            
            int endLine = md.getEnd().map(pos -> pos.line).orElse(currentMethodStart);
            
            MethodMetrics methodMetrics = new MethodMetrics(
                currentMethodName,
                currentClassName,
                currentMethodStart,
                endLine,
                md.getParameters().size()
            );
            
            heatmapData.addMethodMetrics(methodMetrics);
            
            super.visit(md, arg);
        }
        
        private int getLineNumber(Node node) {
            return node.getBegin().map(pos -> pos.line).orElse(0);
        }
    }
    
    private class ComplexityCalculator extends VoidVisitorAdapter<Void> {
        
        private final HeatmapData heatmapData;
        private String currentMethodName;
        private int cyclomaticComplexity;
        
        public ComplexityCalculator(HeatmapData heatmapData) {
            this.heatmapData = heatmapData;
        }
        
        @Override
        public void visit(MethodDeclaration md, Void arg) {
            currentMethodName = md.getNameAsString();
            cyclomaticComplexity = 1;
            
            super.visit(md, arg);
            
            MethodMetrics metrics = heatmapData.getMethodMetrics().stream()
                .filter(m -> m.getMethodName().equals(currentMethodName))
                .findFirst()
                .orElse(null);
            
            if (metrics != null) {
                metrics.setCyclomaticComplexity(cyclomaticComplexity);
            }
        }
        
        @Override
        public void visit(IfStmt n, Void arg) {
            cyclomaticComplexity++;
            heatmapData.incrementLineComplexity(getLineNumber(n));
            super.visit(n, arg);
        }
        
        @Override
        public void visit(ForStmt n, Void arg) {
            cyclomaticComplexity++;
            heatmapData.incrementLineComplexity(getLineNumber(n));
            super.visit(n, arg);
        }
        
        @Override
        public void visit(WhileStmt n, Void arg) {
            cyclomaticComplexity++;
            heatmapData.incrementLineComplexity(getLineNumber(n));
            super.visit(n, arg);
        }
        
        @Override
        public void visit(ForEachStmt n, Void arg) {
            cyclomaticComplexity++;
            heatmapData.incrementLineComplexity(getLineNumber(n));
            super.visit(n, arg);
        }
        
        @Override
        public void visit(DoStmt n, Void arg) {
            cyclomaticComplexity++;
            heatmapData.incrementLineComplexity(getLineNumber(n));
            super.visit(n, arg);
        }
        
        @Override
        public void visit(SwitchStmt n, Void arg) {
            cyclomaticComplexity += n.getEntries().size();
            heatmapData.incrementLineComplexity(getLineNumber(n));
            super.visit(n, arg);
        }
        
        @Override
        public void visit(CatchClause n, Void arg) {
            cyclomaticComplexity++;
            heatmapData.incrementLineComplexity(getLineNumber(n));
            super.visit(n, arg);
        }
        
        @Override
        public void visit(ConditionalExpr n, Void arg) {
            cyclomaticComplexity++;
            heatmapData.incrementLineComplexity(getLineNumber(n));
            super.visit(n, arg);
        }
        
        private int getLineNumber(Node node) {
            return node.getBegin().map(pos -> pos.line).orElse(0);
        }
    }
}