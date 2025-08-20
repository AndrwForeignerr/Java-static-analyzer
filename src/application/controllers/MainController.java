package application.controllers;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import application.models.ClassAnalysisResult;
import application.services.*;

import java.io.File;
import java.util.function.Consumer;

public class MainController {
    
    private DecompilerService decompilerService;
    private AnalyzerService analyzerService;
    private OptimizationService optimizationService;
    private SecurityService securityService;
    private GraphService graphService;
    private HeatmapAnalyzer heatmapAnalyzer;
    
    private Consumer<ClassAnalysisResult> onAnalysisComplete;
    
    public MainController() {
        initializeServices();
    }
    
    private void initializeServices() {
        decompilerService = new DecompilerService();
        analyzerService = new AnalyzerService();
        optimizationService = new OptimizationService();
        securityService = new SecurityService();
        graphService = new GraphService();
        heatmapAnalyzer = new HeatmapAnalyzer();
    }
    
    public void analyzeClassFile(File classFile) {
        Task<ClassAnalysisResult> analysisTask = createAnalysisTask(classFile);
        
        analysisTask.setOnSucceeded(event -> {
            ClassAnalysisResult result = analysisTask.getValue();
            if (onAnalysisComplete != null) {
                onAnalysisComplete.accept(result);
            }
        });
        
        analysisTask.setOnFailed(event -> {
            Throwable exception = analysisTask.getException();
            handleAnalysisError(exception);
        });
        
        Thread analysisThread = new Thread(analysisTask);
        analysisThread.setDaemon(true);
        analysisThread.start();
    }
    
    private Task<ClassAnalysisResult> createAnalysisTask(File classFile) {
        return new Task<ClassAnalysisResult>() {
            @Override
            protected ClassAnalysisResult call() throws Exception {
                updateMessage("Decompiling class file...");
                String decompiledCode = decompilerService.decompile(classFile);
                
                updateMessage("Parsing code...");
                analyzerService.parseCode(decompiledCode);
                
                updateMessage("Running optimization analysis...");
                var optimizations = optimizationService.analyzeOptimizations(analyzerService.getCompilationUnit());
                
                updateMessage("Running security analysis...");
                var securityIssues = securityService.analyzeSecurityIssues(analyzerService.getCompilationUnit());
                
                updateMessage("Generating graphs...");
                var methodCallGraph = graphService.generateMethodCallGraph(analyzerService.getCompilationUnit());
                var classHierarchy = graphService.generateClassHierarchy(analyzerService.getCompilationUnit());
                
                updateMessage("Extracting class information...");
                var classInfo = analyzerService.extractClassInfo();
                
                updateMessage("Generating heatmap data...");
                var heatmapData = heatmapAnalyzer.generateHeatmapData(
                    analyzerService.getCompilationUnit(),
                    optimizations,
                    securityIssues
                );
                
                updateMessage("Analysis complete");
                
                return new ClassAnalysisResult(
                    decompiledCode,
                    optimizations,
                    securityIssues,
                    methodCallGraph,
                    classHierarchy,
                    classInfo,
                    heatmapData
                );
            }
        };
    }
    
    private void handleAnalysisError(Throwable exception) {
        System.err.println("Analysis failed: " + exception.getMessage());
        exception.printStackTrace();
    }
    
    public void setOnAnalysisComplete(Consumer<ClassAnalysisResult> callback) {
        this.onAnalysisComplete = callback;
    }
}