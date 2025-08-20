package application.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import application.models.*;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class HeatmapVisualizationPanel extends VBox {
    
    private Canvas heatmapCanvas;
    private VBox metricsPanel;
    private ComboBox<String> viewModeComboBox;
    private CheckBox showLabelsCheckBox;
    private Slider intensitySlider;
    private Label overallScoreLabel;
    private Label issueCountLabel;
    private Label complexityLabel;
    
    private HeatmapData currentHeatmapData;
    private String[] codeLines;
    
    public HeatmapVisualizationPanel() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }
    
    private void initializeComponents() {
        heatmapCanvas = new Canvas(900, 600);
        metricsPanel = new VBox(10);
        
        viewModeComboBox = new ComboBox<>();
        viewModeComboBox.getItems().addAll("Quality Heatmap", "Complexity View", "Issue Density", "Method Focus");
        viewModeComboBox.setValue("Quality Heatmap");
        
        showLabelsCheckBox = new CheckBox("Show Line Numbers");
        showLabelsCheckBox.setSelected(true);
        
        intensitySlider = new Slider(0.3, 2.0, 1.0);
        intensitySlider.setMajorTickUnit(0.5);
        intensitySlider.setShowTickLabels(true);
        
        overallScoreLabel = new Label("Overall Quality: N/A");
        issueCountLabel = new Label("Total Issues: N/A");
        complexityLabel = new Label("Avg Complexity: N/A");
    }
    
    private void setupLayout() {
        Label titleLabel = new Label("Code Quality Heatmap");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        HBox controlsBox = new HBox(15);
        controlsBox.setAlignment(Pos.CENTER_LEFT);
        controlsBox.setPadding(new Insets(10));
        controlsBox.getChildren().addAll(
            new Label("View:"), viewModeComboBox,
            new Label("Intensity:"), intensitySlider,
            showLabelsCheckBox
        );
        
        VBox statsBox = new VBox(5);
        statsBox.setPadding(new Insets(10));
        statsBox.getChildren().addAll(
            overallScoreLabel,
            issueCountLabel,
            complexityLabel
        );
        
        HBox legendBox = createLegend();
        
        ScrollPane canvasScrollPane = new ScrollPane(heatmapCanvas);
        canvasScrollPane.setFitToWidth(false);
        canvasScrollPane.setFitToHeight(false);
        
        SplitPane mainSplitPane = new SplitPane();
        mainSplitPane.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        mainSplitPane.getItems().addAll(canvasScrollPane, createMetricsPanel());
        mainSplitPane.setDividerPositions(0.7);
        
        this.setPadding(new Insets(10));
        this.getChildren().addAll(titleLabel, controlsBox, statsBox, legendBox, mainSplitPane);
        VBox.setVgrow(mainSplitPane, Priority.ALWAYS);
    }
    
    private HBox createLegend() {
        HBox legendBox = new HBox(20);
        legendBox.setAlignment(Pos.CENTER);
        legendBox.setPadding(new Insets(5));
        
        String[] labels = {"Excellent", "Good", "Average", "Poor", "Critical"};
        Color[] colors = {
            Color.web("#2E7D32"),
            Color.web("#66BB6A"), 
            Color.web("#FFA726"),
            Color.web("#EF5350"),
            Color.web("#C62828")
        };
        
        for (int i = 0; i < labels.length; i++) {
            HBox legendItem = new HBox(5);
            legendItem.setAlignment(Pos.CENTER_LEFT);
            
            Region colorBox = new Region();
            colorBox.setPrefSize(15, 15);
            colorBox.setStyle("-fx-background-color: " + toHexString(colors[i]) + ";");
            
            Label label = new Label(labels[i]);
            label.setFont(Font.font("Arial", 10));
            
            legendItem.getChildren().addAll(colorBox, label);
            legendBox.getChildren().add(legendItem);
        }
        
        return legendBox;
    }
    
    private VBox createMetricsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(250);
        
        Label metricsTitle = new Label("Quality Metrics");
        metricsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        panel.getChildren().add(metricsTitle);
        panel.getChildren().add(metricsPanel);
        
        return panel;
    }
    
    private void setupEventHandlers() {
        viewModeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> redrawHeatmap());
        showLabelsCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> redrawHeatmap());
        intensitySlider.valueProperty().addListener((obs, oldVal, newVal) -> redrawHeatmap());
        
        heatmapCanvas.setOnMouseClicked(event -> {
            if (currentHeatmapData != null && codeLines != null) {
                int lineNumber = getLineNumberFromPosition(event.getX(), event.getY());
                if (lineNumber > 0 && lineNumber <= codeLines.length) {
                    showLineDetails(lineNumber);
                }
            }
        });
    }
    
    public void displayHeatmap(HeatmapData heatmapData, String sourceCode) {
        this.currentHeatmapData = heatmapData;
        this.codeLines = sourceCode != null ? sourceCode.split("\n") : new String[0];
        
        updateStatistics();
        updateMetricsPanel();
        redrawHeatmap();
    }
    
    private void updateStatistics() {
        if (currentHeatmapData == null) {
            overallScoreLabel.setText("Overall Quality: N/A");
            issueCountLabel.setText("Total Issues: N/A");
            complexityLabel.setText("Avg Complexity: N/A");
            return;
        }
        
        double overallScore = currentHeatmapData.getOverallQualityScore();
        int totalIssues = currentHeatmapData.getTotalIssueCount();
        int criticalIssues = currentHeatmapData.getCriticalIssueCount();
        
        double avgComplexity = currentHeatmapData.getMethodMetrics().stream()
            .mapToInt(MethodMetrics::getCyclomaticComplexity)
            .average()
            .orElse(0.0);
        
        overallScoreLabel.setText(String.format("Overall Quality: %.1f%%", overallScore));
        issueCountLabel.setText(String.format("Total Issues: %d (%d critical)", totalIssues, criticalIssues));
        complexityLabel.setText(String.format("Avg Complexity: %.1f", avgComplexity));
        
        overallScoreLabel.setTextFill(getQualityColor(overallScore));
    }
    
    private void updateMetricsPanel() {
        metricsPanel.getChildren().clear();
        
        if (currentHeatmapData == null) {
            Label noDataLabel = new Label("No heatmap data available");
            noDataLabel.setFont(Font.font("Arial", 10));
            metricsPanel.getChildren().add(noDataLabel);
            return;
        }
        
        addMethodMetrics();
        addClassMetrics();
        addHotspotMetrics();
    }
    
    private void addMethodMetrics() {
        Label methodsTitle = new Label("Method Analysis");
        methodsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        metricsPanel.getChildren().add(methodsTitle);
        
        List<MethodMetrics> topComplexMethods = currentHeatmapData.getMethodMetrics().stream()
            .sorted((a, b) -> Integer.compare(b.getCyclomaticComplexity(), a.getCyclomaticComplexity()))
            .limit(5)
            .collect(Collectors.toList());
        
        for (MethodMetrics method : topComplexMethods) {
            VBox methodBox = new VBox(2);
            methodBox.setPadding(new Insets(5));
            methodBox.setStyle("-fx-background-color: #f8f8f8; -fx-background-radius: 3;");
            
            Label methodLabel = new Label(method.getMethodName());
            methodLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            
            Label complexityLabel = new Label("Complexity: " + method.getCyclomaticComplexity());
            complexityLabel.setFont(Font.font("Arial", 9));
            
            Label linesLabel = new Label("Lines: " + method.getLineCount());
            linesLabel.setFont(Font.font("Arial", 9));
            
            methodBox.getChildren().addAll(methodLabel, complexityLabel, linesLabel);
            metricsPanel.getChildren().add(methodBox);
        }
    }
    
    private void addClassMetrics() {
        Label classTitle = new Label("Class Metrics");
        classTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        metricsPanel.getChildren().add(classTitle);
        
        for (ClassMetrics clazz : currentHeatmapData.getClassMetrics()) {
            VBox classBox = new VBox(2);
            classBox.setPadding(new Insets(5));
            classBox.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 3;");
            
            Label classLabel = new Label(clazz.getClassName());
            classLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            
            Label methodsLabel = new Label("Methods: " + clazz.getMethodCount());
            methodsLabel.setFont(Font.font("Arial", 9));
            
            Label fieldsLabel = new Label("Fields: " + clazz.getFieldCount());
            fieldsLabel.setFont(Font.font("Arial", 9));
            
            Double classScore = currentHeatmapData.getClassQualityScores().get(clazz.getClassName());
            if (classScore != null) {
                Label scoreLabel = new Label(String.format("Quality: %.1f%%", classScore));
                scoreLabel.setFont(Font.font("Arial", 9));
                scoreLabel.setTextFill(getQualityColor(classScore));
                classBox.getChildren().add(scoreLabel);
            }
            
            classBox.getChildren().addAll(classLabel, methodsLabel, fieldsLabel);
            metricsPanel.getChildren().add(classBox);
        }
    }
    
    private void addHotspotMetrics() {
        Label hotspotsTitle = new Label("Quality Hotspots");
        hotspotsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        metricsPanel.getChildren().add(hotspotsTitle);
        
        List<HeatmapRegion> criticalRegions = currentHeatmapData.getHeatmapRegions().stream()
            .filter(region -> region.getRegionType() == HeatmapRegionType.CRITICAL || 
                             region.getRegionType() == HeatmapRegionType.POOR)
            .sorted((a, b) -> Double.compare(a.getQualityScore(), b.getQualityScore()))
            .limit(5)
            .collect(Collectors.toList());
        
        for (HeatmapRegion region : criticalRegions) {
            VBox regionBox = new VBox(2);
            regionBox.setPadding(new Insets(5));
            regionBox.setStyle("-fx-background-color: #ffe6e6; -fx-background-radius: 3;");
            
            Label regionLabel = new Label(String.format("Lines %d-%d", region.getStartLine(), region.getEndLine()));
            regionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            
            Label typeLabel = new Label("Type: " + region.getRegionType());
            typeLabel.setFont(Font.font("Arial", 9));
            
            Label scoreLabel = new Label(String.format("Score: %.1f%%", region.getQualityScore()));
            scoreLabel.setFont(Font.font("Arial", 9));
            scoreLabel.setTextFill(getQualityColor(region.getQualityScore()));
            
            regionBox.getChildren().addAll(regionLabel, typeLabel, scoreLabel);
            metricsPanel.getChildren().add(regionBox);
        }
    }
    
    private void redrawHeatmap() {
        if (currentHeatmapData == null || codeLines == null) {
            clearCanvas();
            return;
        }
        
        String viewMode = viewModeComboBox.getValue();
        switch (viewMode) {
            case "Quality Heatmap":
                drawQualityHeatmap();
                break;
            case "Complexity View":
                drawComplexityView();
                break;
            case "Issue Density":
                drawIssueDensity();
                break;
            case "Method Focus":
                drawMethodFocus();
                break;
        }
    }
    
    private void drawQualityHeatmap() {
        GraphicsContext gc = heatmapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, heatmapCanvas.getWidth(), heatmapCanvas.getHeight());
        
        double lineHeight = 16;
        double lineNumberWidth = showLabelsCheckBox.isSelected() ? 60 : 10;
        double codeWidth = heatmapCanvas.getWidth() - lineNumberWidth - 20;
        double intensity = intensitySlider.getValue();
        
        adjustCanvasSize(lineHeight);
        
        for (int i = 0; i < codeLines.length; i++) {
            int lineNumber = i + 1;
            double y = i * lineHeight + 10;
            
            Double qualityScore = currentHeatmapData.getLineQualityScores().get(lineNumber);
            Color lineColor = getHeatmapColor(qualityScore, intensity);
            
            gc.setFill(lineColor);
            gc.fillRect(10, y, codeWidth + lineNumberWidth, lineHeight - 1);
            
            if (showLabelsCheckBox.isSelected()) {
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font("Courier New", 10));
                gc.fillText(String.valueOf(lineNumber), 15, y + 12);
            }
            
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Courier New", 10));
            String lineText = codeLines[i];
            if (lineText.length() > 80) {
                lineText = lineText.substring(0, 77) + "...";
            }
            gc.fillText(lineText, lineNumberWidth + 15, y + 12);
        }
        
        drawRegionBorders(gc, lineHeight, lineNumberWidth, codeWidth);
    }
    
    private void drawComplexityView() {
        GraphicsContext gc = heatmapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, heatmapCanvas.getWidth(), heatmapCanvas.getHeight());
        
        double lineHeight = 16;
        double lineNumberWidth = showLabelsCheckBox.isSelected() ? 60 : 10;
        double codeWidth = heatmapCanvas.getWidth() - lineNumberWidth - 20;
        
        adjustCanvasSize(lineHeight);
        
        for (int i = 0; i < codeLines.length; i++) {
            int lineNumber = i + 1;
            double y = i * lineHeight + 10;
            
            int complexity = currentHeatmapData.getLineComplexity(lineNumber);
            Color complexityColor = getComplexityColor(complexity);
            
            gc.setFill(complexityColor);
            gc.fillRect(10, y, codeWidth + lineNumberWidth, lineHeight - 1);
            
            if (showLabelsCheckBox.isSelected()) {
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font("Courier New", 10));
                gc.fillText(String.valueOf(lineNumber), 15, y + 12);
            }
            
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Courier New", 10));
            String lineText = codeLines[i];
            if (lineText.length() > 80) {
                lineText = lineText.substring(0, 77) + "...";
            }
            gc.fillText(lineText, lineNumberWidth + 15, y + 12);
            
            if (complexity > 0) {
                gc.setFill(Color.DARKRED);
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 8));
                gc.fillText("C:" + complexity, codeWidth + lineNumberWidth - 30, y + 10);
            }
        }
    }
    
    private void drawIssueDensity() {
        GraphicsContext gc = heatmapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, heatmapCanvas.getWidth(), heatmapCanvas.getHeight());
        
        double lineHeight = 16;
        double lineNumberWidth = showLabelsCheckBox.isSelected() ? 60 : 10;
        double codeWidth = heatmapCanvas.getWidth() - lineNumberWidth - 20;
        
        adjustCanvasSize(lineHeight);
        
        for (int i = 0; i < codeLines.length; i++) {
            int lineNumber = i + 1;
            double y = i * lineHeight + 10;
            
            List<CodeIssue> issues = currentHeatmapData.getIssueDistribution().get(lineNumber);
            Color issueColor = getIssueDensityColor(issues);
            
            gc.setFill(issueColor);
            gc.fillRect(10, y, codeWidth + lineNumberWidth, lineHeight - 1);
            
            if (showLabelsCheckBox.isSelected()) {
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font("Courier New", 10));
                gc.fillText(String.valueOf(lineNumber), 15, y + 12);
            }
            
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Courier New", 10));
            String lineText = codeLines[i];
            if (lineText.length() > 80) {
                lineText = lineText.substring(0, 77) + "...";
            }
            gc.fillText(lineText, lineNumberWidth + 15, y + 12);
            
            if (issues != null && !issues.isEmpty()) {
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 8));
                gc.fillText("(" + issues.size() + ")", codeWidth + lineNumberWidth - 25, y + 10);
            }
        }
    }
    
    private void drawMethodFocus() {
        GraphicsContext gc = heatmapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, heatmapCanvas.getWidth(), heatmapCanvas.getHeight());
        
        double lineHeight = 16;
        double lineNumberWidth = showLabelsCheckBox.isSelected() ? 60 : 10;
        double codeWidth = heatmapCanvas.getWidth() - lineNumberWidth - 20;
        
        adjustCanvasSize(lineHeight);
        
        for (int i = 0; i < codeLines.length; i++) {
            int lineNumber = i + 1;
            double y = i * lineHeight + 10;
            
            MethodMetrics method = getMethodForLine(lineNumber);
            Color methodColor = getMethodFocusColor(method, lineNumber);
            
            gc.setFill(methodColor);
            gc.fillRect(10, y, codeWidth + lineNumberWidth, lineHeight - 1);
            
            if (showLabelsCheckBox.isSelected()) {
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font("Courier New", 10));
                gc.fillText(String.valueOf(lineNumber), 15, y + 12);
            }
            
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Courier New", 10));
            String lineText = codeLines[i];
            if (lineText.length() > 80) {
                lineText = lineText.substring(0, 77) + "...";
            }
            gc.fillText(lineText, lineNumberWidth + 15, y + 12);
        }
        
        drawMethodBoundaries(gc, lineHeight, lineNumberWidth, codeWidth);
    }
    
    private void drawRegionBorders(GraphicsContext gc, double lineHeight, double lineNumberWidth, double codeWidth) {
        gc.setStroke(Color.DARKBLUE);
        gc.setLineWidth(2);
        
        for (HeatmapRegion region : currentHeatmapData.getHeatmapRegions()) {
            if (region.getRegionType() == HeatmapRegionType.CRITICAL) {
                double startY = (region.getStartLine() - 1) * lineHeight + 10;
                double endY = region.getEndLine() * lineHeight + 10;
                
                gc.strokeRect(8, startY - 2, codeWidth + lineNumberWidth + 4, endY - startY + 2);
            }
        }
    }
    
    private void drawMethodBoundaries(GraphicsContext gc, double lineHeight, double lineNumberWidth, double codeWidth) {
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(1);
        
        for (MethodMetrics method : currentHeatmapData.getMethodMetrics()) {
            double startY = (method.getStartLine() - 1) * lineHeight + 10;
            double endY = method.getEndLine() * lineHeight + 10;
            
            gc.strokeLine(lineNumberWidth + 5, startY, lineNumberWidth + 5, endY);
            gc.strokeLine(lineNumberWidth + 5, startY, lineNumberWidth + 15, startY);
            gc.strokeLine(lineNumberWidth + 5, endY, lineNumberWidth + 15, endY);
        }
    }
    
    private void adjustCanvasSize(double lineHeight) {
        double requiredHeight = Math.max(600, codeLines.length * lineHeight + 20);
        if (heatmapCanvas.getHeight() != requiredHeight) {
            heatmapCanvas.setHeight(requiredHeight);
        }
    }
    
    private void clearCanvas() {
        GraphicsContext gc = heatmapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, heatmapCanvas.getWidth(), heatmapCanvas.getHeight());
        
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Arial", 14));
        gc.fillText("No heatmap data available", 50, 50);
    }
    
    private Color getHeatmapColor(Double qualityScore, double intensity) {
        if (qualityScore == null) {
            return Color.WHITE;
        }
        
        Color baseColor;
        if (qualityScore >= 80) baseColor = Color.web("#2E7D32");
        else if (qualityScore >= 60) baseColor = Color.web("#66BB6A");
        else if (qualityScore >= 40) baseColor = Color.web("#FFA726");
        else if (qualityScore >= 20) baseColor = Color.web("#EF5350");
        else baseColor = Color.web("#C62828");
        
        double alpha = Math.min(1.0, (100 - qualityScore) / 100.0 * intensity);
        return Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
    }
    
    private Color getComplexityColor(int complexity) {
        if (complexity == 0) return Color.WHITE;
        if (complexity == 1) return Color.web("#E8F5E8");
        if (complexity <= 3) return Color.web("#FFEB3B");
        if (complexity <= 5) return Color.web("#FF9800");
        return Color.web("#F44336");
    }
    
    private Color getIssueDensityColor(List<CodeIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return Color.WHITE;
        }
        
        int criticalCount = (int) issues.stream().filter(i -> i.getSeverity() == IssueSeverity.CRITICAL).count();
        int highCount = (int) issues.stream().filter(i -> i.getSeverity() == IssueSeverity.HIGH).count();
        
        if (criticalCount > 0) return Color.web("#C62828");
        if (highCount > 0) return Color.web("#EF5350");
        if (issues.size() > 2) return Color.web("#FFA726");
        if (issues.size() > 1) return Color.web("#66BB6A");
        return Color.web("#E8F5E8");
    }
    
    private Color getMethodFocusColor(MethodMetrics method, int lineNumber) {
        if (method == null) {
            return Color.WHITE;
        }
        
        Double methodScore = currentHeatmapData.getMethodQualityScores().get(method.getMethodName());
        if (methodScore == null) {
            return Color.LIGHTGRAY;
        }
        
        if (lineNumber == method.getStartLine()) {
            return Color.LIGHTBLUE;
        }
        
        return getHeatmapColor(methodScore, 0.5);
    }
    
    private Color getQualityColor(double score) {
        if (score >= 80) return Color.GREEN;
        if (score >= 60) return Color.ORANGE;
        if (score >= 40) return Color.DARKORANGE;
        return Color.RED;
    }
    
    private MethodMetrics getMethodForLine(int lineNumber) {
        return currentHeatmapData.getMethodMetrics().stream()
            .filter(method -> lineNumber >= method.getStartLine() && lineNumber <= method.getEndLine())
            .findFirst()
            .orElse(null);
    }
    
    private int getLineNumberFromPosition(double x, double y) {
        double lineHeight = 16;
        int lineIndex = (int) ((y - 10) / lineHeight);
        return lineIndex + 1;
    }
    
    private void showLineDetails(int lineNumber) {
        if (currentHeatmapData == null) return;
        
        StringBuilder details = new StringBuilder();
        details.append("Line ").append(lineNumber).append(" Details:\n\n");
        
        Double qualityScore = currentHeatmapData.getLineQualityScores().get(lineNumber);
        if (qualityScore != null) {
            details.append("Quality Score: ").append(String.format("%.1f%%", qualityScore)).append("\n");
        }
        
        int complexity = currentHeatmapData.getLineComplexity(lineNumber);
        if (complexity > 0) {
            details.append("Complexity: ").append(complexity).append("\n");
        }
        
        List<CodeIssue> issues = currentHeatmapData.getIssueDistribution().get(lineNumber);
        if (issues != null && !issues.isEmpty()) {
            details.append("\nIssues:\n");
            for (CodeIssue issue : issues) {
                details.append("- ").append(issue.getType()).append(" (").append(issue.getSeverity()).append("): ");
                details.append(issue.getDescription()).append("\n");
            }
        }
        
        MethodMetrics method = getMethodForLine(lineNumber);
        if (method != null) {
            details.append("\nMethod: ").append(method.getMethodName()).append("\n");
            details.append("Method Complexity: ").append(method.getCyclomaticComplexity()).append("\n");
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Line Details");
        alert.setHeaderText(null);
        alert.setContentText(details.toString());
        alert.showAndWait();
    }
    
    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }
}