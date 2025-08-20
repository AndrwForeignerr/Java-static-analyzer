package application.ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

public class StatusBar extends HBox {
    
    private Label statusLabel;
    private ProgressBar progressBar;
    
    public StatusBar() {
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        statusLabel = new Label("Ready");
        progressBar = new ProgressBar();
        progressBar.setVisible(false);
    }
    
    private void setupLayout() {
        this.setPadding(new Insets(5, 10, 5, 10));
        this.setSpacing(10);
        this.getChildren().addAll(statusLabel, progressBar);
        
        this.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, 
                                                 CornerRadii.EMPTY, new BorderWidths(1, 0, 0, 0))));
    }
    
    public void setStatus(String status) {
        statusLabel.setText(status);
    }
    
    public void showProgress() {
        progressBar.setVisible(true);
    }
    
    public void hideProgress() {
        progressBar.setVisible(false);
    }
}