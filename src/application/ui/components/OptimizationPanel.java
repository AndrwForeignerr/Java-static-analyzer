package application.ui.components;

import java.util.List;

import application.models.OptimizationSeverity;
import application.models.OptimizationSuggestion;
import application.ui.components.OptimizationPanel.OptimizationListCell;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class OptimizationPanel extends VBox {
    
    private ListView<OptimizationSuggestion> optimizationListView;
    private TextArea detailsTextArea;
    
    public OptimizationPanel() {
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        optimizationListView = new ListView<>();
        optimizationListView.setCellFactory(listView -> new OptimizationListCell());
        
        detailsTextArea = new TextArea();
        detailsTextArea.setEditable(false);
        detailsTextArea.setWrapText(true);
        
        optimizationListView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    displayOptimizationDetails(newValue);
                }
            }
        );
    }
    
    private void setupLayout() {
        Label titleLabel = new Label("Optimization Suggestions");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.getItems().addAll(optimizationListView, detailsTextArea);
        splitPane.setDividerPositions(0.6);
        
        this.setPadding(new Insets(10));
        this.getChildren().addAll(titleLabel, splitPane);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
    }
    
    public void displayOptimizations(List<OptimizationSuggestion> optimizations) {
        optimizationListView.getItems().clear();
        optimizationListView.getItems().addAll(optimizations);
    }
    
    private void displayOptimizationDetails(OptimizationSuggestion optimization) {
        StringBuilder details = new StringBuilder();
        details.append("Type: ").append(optimization.getType()).append("\n\n");
        details.append("Line: ").append(optimization.getLineNumber()).append("\n\n");
        details.append("Description: ").append(optimization.getDescription()).append("\n\n");
        details.append("Original Code:\n").append(optimization.getOriginalCode()).append("\n\n");
        details.append("Suggested Code:\n").append(optimization.getSuggestedCode()).append("\n\n");
        details.append("Severity: ").append(optimization.getSeverity());
        
        detailsTextArea.setText(details.toString());
    }
    
    public class OptimizationListCell extends ListCell<OptimizationSuggestion> {
        @Override
        protected void updateItem(OptimizationSuggestion item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText("Line " + item.getLineNumber() + ": " + item.getType() + 
                       " (" + item.getSeverity() + ")");
                
                Color severityColor = getSeverityColor(item.getSeverity());
                setStyle("-fx-text-fill: " + toHexString(severityColor) + ";");
            }
        }
        
        private Color getSeverityColor(OptimizationSeverity severity) {
            switch (severity) {
                case CRITICAL: return Color.DARKRED;
                case HIGH: return Color.RED;
                case MEDIUM: return Color.ORANGE;
                case LOW: return Color.BLUE;
                default: return Color.BLACK;
            }
        }
        
        private String toHexString(Color color) {
            return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
        }
    }
}

