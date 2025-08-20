package application.ui.components;

import java.util.List;

import application.models.SecurityIssue;
import application.models.SecuritySeverity;
import application.ui.components.SecurityListCell;
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

public class SecurityPanel extends VBox {
    
    private ListView<SecurityIssue> securityListView;
    private TextArea detailsTextArea;
    
    public SecurityPanel() {
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        securityListView = new ListView<>();
        securityListView.setCellFactory(listView -> new SecurityListCell());
        
        detailsTextArea = new TextArea();
        detailsTextArea.setEditable(false);
        detailsTextArea.setWrapText(true);
        
        securityListView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    displaySecurityDetails(newValue);
                }
            }
        );
    }
    
    private void setupLayout() {
        Label titleLabel = new Label("Security Issues");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.getItems().addAll(securityListView, detailsTextArea);
        splitPane.setDividerPositions(0.6);
        
        this.setPadding(new Insets(10));
        this.getChildren().addAll(titleLabel, splitPane);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
    }
    
    public void displaySecurityIssues(List<SecurityIssue> securityIssues) {
        securityListView.getItems().clear();
        securityListView.getItems().addAll(securityIssues);
    }
    
    private void displaySecurityDetails(SecurityIssue securityIssue) {
        StringBuilder details = new StringBuilder();
        details.append("Type: ").append(securityIssue.getType()).append("\n\n");
        details.append("Line: ").append(securityIssue.getLineNumber()).append("\n\n");
        details.append("Description: ").append(securityIssue.getDescription()).append("\n\n");
        details.append("Vulnerable Code:\n").append(securityIssue.getVulnerableCode()).append("\n\n");
        details.append("Recommendation:\n").append(securityIssue.getRecommendation()).append("\n\n");
        details.append("Severity: ").append(securityIssue.getSeverity());
        
        detailsTextArea.setText(details.toString());
    }
    

}
