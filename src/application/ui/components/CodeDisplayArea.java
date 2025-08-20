package application.ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import application.models.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class CodeDisplayArea extends VBox {
    
    private ScrollPane codeScrollPane;
    private VBox codeContainer;
    private ScrollPane annotationsScrollPane;
    private VBox annotationsPane;
    
    private Map<Integer, Color> lineColors = new HashMap<>();
    private Map<Integer, String> lineIssueTypes = new HashMap<>();
    
    public CodeDisplayArea() {
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        codeContainer = new VBox();
        codeContainer.setPadding(new Insets(10));
        codeContainer.setSpacing(2);
        
        codeScrollPane = new ScrollPane(codeContainer);
        codeScrollPane.setFitToWidth(true);
        codeScrollPane.setFitToHeight(true);
        codeScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        codeScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        annotationsPane = new VBox(5);
        annotationsPane.setPadding(new Insets(10));
        annotationsPane.setFillWidth(true);
        
        annotationsScrollPane = new ScrollPane(annotationsPane);
        annotationsScrollPane.setFitToWidth(true);
        annotationsScrollPane.setFitToHeight(false);
        annotationsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        annotationsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        annotationsScrollPane.setPrefViewportHeight(400);
        annotationsScrollPane.setPrefViewportWidth(350);
    }
    
    private void setupLayout() {
        Label titleLabel = new Label("Decompiled Code");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        splitPane.getItems().addAll(codeScrollPane, annotationsScrollPane);
        splitPane.setDividerPositions(0.65);
        
        this.setPadding(new Insets(10));
        this.getChildren().addAll(titleLabel, splitPane);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
    }
    
    public void displayCode(String code, List<OptimizationSuggestion> optimizations, 
                           List<SecurityIssue> securityIssues) {
        
        prepareLineColoring(optimizations, securityIssues);
        displayCodeWithLineNumbers(code);
        displayAnnotations(optimizations, securityIssues);
    }
    
    private void prepareLineColoring(List<OptimizationSuggestion> optimizations, 
                                   List<SecurityIssue> securityIssues) {
        lineColors.clear();
        lineIssueTypes.clear();
        
        for (OptimizationSuggestion opt : optimizations) {
            int lineNum = opt.getLineNumber();
            Color color = getOptimizationColor(opt.getSeverity());
            lineColors.put(lineNum, color);
            lineIssueTypes.put(lineNum, "OPTIMIZATION");
        }
        
        for (SecurityIssue sec : securityIssues) {
            int lineNum = sec.getLineNumber();
            Color color = getSecurityColor(sec.getSeverity());
            lineColors.put(lineNum, color);
            lineIssueTypes.put(lineNum, "SECURITY");
        }
    }
    
    private void displayCodeWithLineNumbers(String code) {
        codeContainer.getChildren().clear();
        
        if (code == null || code.trim().isEmpty()) {
            Label noCodeLabel = new Label("No code to display");
            noCodeLabel.setFont(Font.font("Courier New", 12));
            noCodeLabel.setTextFill(Color.GRAY);
            codeContainer.getChildren().add(noCodeLabel);
            return;
        }
        
        String[] lines = code.split("\n");
        int maxLineNumWidth = String.valueOf(lines.length).length();
        
        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            String lineText = lines[i];
            
            HBox lineContainer = createCodeLine(lineNumber, lineText, maxLineNumWidth);
            codeContainer.getChildren().add(lineContainer);
        }
    }
    
    private HBox createCodeLine(int lineNumber, String lineText, int maxLineNumWidth) {
        HBox lineContainer = new HBox();
        lineContainer.setSpacing(10);
        
        String lineNumText = String.format("%" + maxLineNumWidth + "d", lineNumber);
        Label lineNumLabel = new Label(lineNumText);
        lineNumLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
        lineNumLabel.setTextFill(Color.GRAY);
        lineNumLabel.setMinWidth(maxLineNumWidth * 10 + 10);
        lineNumLabel.setMaxWidth(maxLineNumWidth * 10 + 10);
        
        Region issueIndicator = new Region();
        issueIndicator.setPrefWidth(20);
        issueIndicator.setPrefHeight(16);
        
        if (lineColors.containsKey(lineNumber)) {
            Color issueColor = lineColors.get(lineNumber);
            Color softerColor = Color.color(
                issueColor.getRed() * 0.7 + 0.3,
                issueColor.getGreen() * 0.7 + 0.3,
                issueColor.getBlue() * 0.7 + 0.3,
                0.8
            );
            issueIndicator.setStyle("-fx-background-color: " + toHexString(softerColor) + "; " +
                                   "-fx-background-radius: 3; " +
                                   "-fx-border-color: " + toHexString(softerColor.darker()) + "; " +
                                   "-fx-border-width: 1; " +
                                   "-fx-border-radius: 3;");
            
            String issueType = lineIssueTypes.get(lineNumber);
            Tooltip tooltip = new Tooltip(issueType + " issue on line " + lineNumber);
            Tooltip.install(issueIndicator, tooltip);
        } else {
            issueIndicator.setStyle("-fx-background-color: transparent;");
        }
        
        TextFlow codeTextFlow = createSyntaxHighlightedText(lineText);
        
        if (lineColors.containsKey(lineNumber)) {
            Color bgColor = lineColors.get(lineNumber);
            Color lightBgColor = Color.color(
                bgColor.getRed() * 0.3 + 0.7,
                bgColor.getGreen() * 0.3 + 0.7, 
                bgColor.getBlue() * 0.3 + 0.7, 
                0.15
            );
            lineContainer.setStyle("-fx-background-color: " + toHexString(lightBgColor) + "; " +
                                 "-fx-background-radius: 3; " +
                                 "-fx-padding: 2;");
        }
        
        lineContainer.getChildren().addAll(lineNumLabel, issueIndicator, codeTextFlow);
        return lineContainer;
    }
    
    private TextFlow createSyntaxHighlightedText(String lineText) {
        TextFlow textFlow = new TextFlow();
        textFlow.setMaxWidth(Double.MAX_VALUE);
        
        if (lineText.trim().isEmpty()) {
            Text emptyText = new Text(" ");
            emptyText.setFont(Font.font("Courier New", 12));
            textFlow.getChildren().add(emptyText);
            return textFlow;
        }
        
        Text codeText = new Text(lineText);
        codeText.setFont(Font.font("Courier New", 12));
        
        if (lineText.trim().startsWith("//") || lineText.trim().startsWith("/*")) {
            codeText.setFill(Color.GREEN.darker());
        } else if (lineText.contains("public ") || lineText.contains("private ") || 
                  lineText.contains("protected ") || lineText.contains("static ")) {
            codeText.setFill(Color.BLUE.darker());
        } else if (lineText.contains("\"")) {
            codeText.setFill(Color.CHOCOLATE);
        } else {
            codeText.setFill(Color.BLACK);
        }
        
        textFlow.getChildren().add(codeText);
        return textFlow;
    }
    
    private void displayAnnotations(List<OptimizationSuggestion> optimizations, 
                                  List<SecurityIssue> securityIssues) {
        annotationsPane.getChildren().clear();
        
        Label annotationsTitle = new Label("Code Annotations");
        annotationsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        annotationsPane.getChildren().add(annotationsTitle);
        
        if (optimizations.isEmpty() && securityIssues.isEmpty()) {
            Label noAnnotationsLabel = new Label("No issues found");
            noAnnotationsLabel.setFont(Font.font("Arial", 10));
            noAnnotationsLabel.setTextFill(Color.GRAY);
            annotationsPane.getChildren().add(noAnnotationsLabel);
            return;
        }
        
        Map<Integer, VBox> annotationsByLine = new HashMap<>();
        
        for (OptimizationSuggestion opt : optimizations) {
            int lineNum = opt.getLineNumber();
            VBox lineAnnotations = annotationsByLine.computeIfAbsent(lineNum, k -> new VBox(3));
            
            VBox optBox = createAnnotationBox("OPTIMIZATION", opt.getDescription(), 
                                            lineNum, getOptimizationColor(opt.getSeverity()));
            lineAnnotations.getChildren().add(optBox);
        }
        
        for (SecurityIssue sec : securityIssues) {
            int lineNum = sec.getLineNumber();
            VBox lineAnnotations = annotationsByLine.computeIfAbsent(lineNum, k -> new VBox(3));
            
            VBox secBox = createAnnotationBox("SECURITY", sec.getDescription(), 
                                            lineNum, getSecurityColor(sec.getSeverity()));
            lineAnnotations.getChildren().add(secBox);
        }
        
        annotationsByLine.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                annotationsPane.getChildren().add(entry.getValue());
                
                Separator separator = new Separator();
                separator.setStyle("-fx-background-color: #e0e0e0;");
                annotationsPane.getChildren().add(separator);
            });
    }
    
    private VBox createAnnotationBox(String type, String description, int lineNumber, Color color) {
        VBox box = new VBox(3);
        box.setPadding(new Insets(8));
        box.setMaxWidth(Double.MAX_VALUE);
        box.setBorder(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, 
                                                new CornerRadii(3), BorderWidths.DEFAULT)));
        
        HBox headerBox = new HBox(8);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Region colorIndicator = new Region();
        colorIndicator.setPrefSize(12, 12);
        colorIndicator.setStyle("-fx-background-color: " + toHexString(color) + "; " +
                               "-fx-background-radius: 6;");
        
        Label typeLabel = new Label(type + " - Line " + lineNumber);
        typeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        typeLabel.setTextFill(color.darker());
        
        headerBox.getChildren().addAll(colorIndicator, typeLabel);
        
        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("Arial", 9));
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(320);
        
        box.getChildren().addAll(headerBox, descLabel);
        return box;
    }
    
    private Color getOptimizationColor(OptimizationSeverity severity) {
        switch (severity) {
            case CRITICAL: return Color.rgb(139, 0, 0, 0.7);
            case HIGH: return Color.rgb(205, 92, 92, 0.7);
            case MEDIUM: return Color.rgb(255, 160, 122, 0.7);
            case LOW: return Color.rgb(135, 206, 250, 0.7);
            default: return Color.rgb(128, 128, 128, 0.5);
        }
    }
    
    private Color getSecurityColor(SecuritySeverity severity) {
        switch (severity) {
            case CRITICAL: return Color.rgb(139, 0, 0, 0.7);
            case HIGH: return Color.rgb(178, 34, 34, 0.7);
            case MEDIUM: return Color.rgb(210, 105, 30, 0.7);
            case LOW: return Color.rgb(147, 112, 219, 0.7);
            default: return Color.rgb(128, 128, 128, 0.5);
        }
    }
    
    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }
}