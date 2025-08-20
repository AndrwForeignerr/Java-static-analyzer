package application.ui.components;

import application.models.SecurityIssue;
import application.models.SecuritySeverity;
import javafx.scene.control.ListCell;
import javafx.scene.paint.Color;

public class SecurityListCell extends ListCell<SecurityIssue> {
    @Override
    protected void updateItem(SecurityIssue item, boolean empty) {
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
    
    private Color getSeverityColor(SecuritySeverity severity) {
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