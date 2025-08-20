package application;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import application.controllers.MainController;
import application.models.ClassAnalysisResult;
import application.services.DecompilerService;
import application.services.AnalyzerService;
import application.ui.components.*;

import java.io.File;

public class Main extends Application {
    
    private MainController mainController;
    private Stage primaryStage;
    private BorderPane rootLayout;
    private TabPane mainTabPane;
    
    private CodeDisplayArea codeDisplayArea;
    private OptimizationPanel optimizationPanel;
    private SecurityPanel securityPanel;
    private GraphVisualizationPanel graphPanel;
    private ClassInfoPanel classInfoPanel;
    private HeatmapVisualizationPanel heatmapPanel;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            this.primaryStage = primaryStage;
            this.mainController = new MainController();
            
            initializeComponents();
            setupLayout();
            setupEventHandlers();
            
            Scene scene = new Scene(rootLayout, 1400, 900);
            scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
            
            primaryStage.setTitle("Java Static Analyzer");
            primaryStage.setScene(scene);
            primaryStage.show();
            
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private void initializeComponents() {
        rootLayout = new BorderPane();
        mainTabPane = new TabPane();
        
        codeDisplayArea = new CodeDisplayArea();
        optimizationPanel = new OptimizationPanel();
        securityPanel = new SecurityPanel();
        graphPanel = new GraphVisualizationPanel();
        classInfoPanel = new ClassInfoPanel();
        heatmapPanel = new HeatmapVisualizationPanel();
    }
    
    private void setupLayout() {
        VBox topPanel = createTopPanel();
        rootLayout.setTop(topPanel);
        
        setupMainTabs();
        rootLayout.setCenter(mainTabPane);
        
        StatusBar statusBar = new StatusBar();
        rootLayout.setBottom(statusBar);
    }
    
    private VBox createTopPanel() {
        VBox topPanel = new VBox(10);
        topPanel.setPadding(new Insets(10));
        
        HBox fileSelectionBox = new HBox(10);
        fileSelectionBox.setAlignment(Pos.CENTER_LEFT);
        
        Button selectFileButton = new Button("Select Class File");
        selectFileButton.setOnAction(e -> selectClassFile());
        
        Label filePathLabel = new Label("No file selected");
        filePathLabel.setId("filePathLabel");
        
        fileSelectionBox.getChildren().addAll(selectFileButton, filePathLabel);
        topPanel.getChildren().add(fileSelectionBox);
        
        return topPanel;
    }
    
    private void setupMainTabs() {
        Tab codeTab = new Tab("Decompiled Code");
        codeTab.setContent(codeDisplayArea);
        codeTab.setClosable(false);
        
        Tab optimizationTab = new Tab("Optimization Analysis");
        optimizationTab.setContent(optimizationPanel);
        optimizationTab.setClosable(false);
        
        Tab securityTab = new Tab("Security Analysis");
        securityTab.setContent(securityPanel);
        securityTab.setClosable(false);
        
        Tab graphTab = new Tab("Graph Visualization");
        graphTab.setContent(graphPanel);
        graphTab.setClosable(false);
        
        Tab classInfoTab = new Tab("Class Information");
        classInfoTab.setContent(classInfoPanel);
        classInfoTab.setClosable(false);
        
        Tab heatmapTab = new Tab("Quality Heatmap");
        heatmapTab.setContent(heatmapPanel);
        heatmapTab.setClosable(false);
        
        mainTabPane.getTabs().addAll(codeTab, optimizationTab, securityTab, graphTab, classInfoTab, heatmapTab);
    }
    
    private void setupEventHandlers() {
        mainController.setOnAnalysisComplete(this::onAnalysisComplete);
    }
    
    private void selectClassFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Class File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Java Class Files", "*.class")
        );
        
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            processClassFile(selectedFile);
        }
    }
    
    private void processClassFile(File classFile) {
        Label filePathLabel = (Label) rootLayout.getTop().lookup("#filePathLabel");
        filePathLabel.setText(classFile.getAbsolutePath());
        
        mainController.analyzeClassFile(classFile);
    }
    
    private void onAnalysisComplete(ClassAnalysisResult result) {
        codeDisplayArea.displayCode(result.getDecompiledCode(), 
                                  result.getOptimizationSuggestions(), 
                                  result.getSecurityIssues());
        
        optimizationPanel.displayOptimizations(result.getOptimizationSuggestions());
        securityPanel.displaySecurityIssues(result.getSecurityIssues());
        graphPanel.displayGraphs(result.getMethodCallGraph(), result.getClassHierarchy());
        classInfoPanel.displayClassInfo(result.getClassInfo());
        heatmapPanel.displayHeatmap(result.getHeatmapData(), result.getDecompiledCode());
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}