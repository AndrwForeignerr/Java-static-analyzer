package application.ui.components;

import application.models.ClassInfo;
import application.models.FieldInfo;
import application.models.MethodInfo;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ClassInfoPanel extends VBox {
    
    private TableView<FieldInfo> fieldsTable;
    private TableView<MethodInfo> methodsTable;
    private TextArea classDetailsArea;
    
    public ClassInfoPanel() {
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        fieldsTable = new TableView<>();
        setupFieldsTable();
        
        methodsTable = new TableView<>();
        setupMethodsTable();
        
        classDetailsArea = new TextArea();
        classDetailsArea.setEditable(false);
        classDetailsArea.setPrefRowCount(5);
    }
    
    private void setupFieldsTable() {
        TableColumn<FieldInfo, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        
        TableColumn<FieldInfo, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getType()));
        
        TableColumn<FieldInfo, String> modifierCol = new TableColumn<>("Access");
        modifierCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getAccessModifier()));
        
        TableColumn<FieldInfo, String> staticCol = new TableColumn<>("Static");
        staticCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().isStatic() ? "Yes" : "No"));
        
        TableColumn<FieldInfo, String> finalCol = new TableColumn<>("Final");
        finalCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().isFinal() ? "Yes" : "No"));
        
        fieldsTable.getColumns().addAll(nameCol, typeCol, modifierCol, staticCol, finalCol);
    }
    
    private void setupMethodsTable() {
        TableColumn<MethodInfo, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        
        TableColumn<MethodInfo, String> returnCol = new TableColumn<>("Return Type");
        returnCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getReturnType()));
        
        TableColumn<MethodInfo, String> paramsCol = new TableColumn<>("Parameters");
        paramsCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            String.join(", ", data.getValue().getParameters())));
        
        TableColumn<MethodInfo, String> modifierCol = new TableColumn<>("Access");
        modifierCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getAccessModifier()));
        
        methodsTable.getColumns().addAll(nameCol, returnCol, paramsCol, modifierCol);
    }
    
    private void setupLayout() {
        Label titleLabel = new Label("Class Information");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        TabPane infoTabPane = new TabPane();
        
        Tab fieldsTab = new Tab("Fields");
        fieldsTab.setContent(fieldsTable);
        fieldsTab.setClosable(false);
        
        Tab methodsTab = new Tab("Methods");
        methodsTab.setContent(methodsTable);
        methodsTab.setClosable(false);
        
        Tab detailsTab = new Tab("Class Details");
        detailsTab.setContent(classDetailsArea);
        detailsTab.setClosable(false);
        
        infoTabPane.getTabs().addAll(fieldsTab, methodsTab, detailsTab);
        
        this.setPadding(new Insets(10));
        this.getChildren().addAll(titleLabel, infoTabPane);
        VBox.setVgrow(infoTabPane, Priority.ALWAYS);
    }
    
    public void displayClassInfo(ClassInfo classInfo) {
        if (classInfo == null) {
            fieldsTable.getItems().clear();
            methodsTable.getItems().clear();
            classDetailsArea.setText("No class information available");
            return;
        }
        
        fieldsTable.getItems().clear();
        fieldsTable.getItems().addAll(classInfo.getFields());
        
        methodsTable.getItems().clear();
        methodsTable.getItems().addAll(classInfo.getMethods());
        
        StringBuilder details = new StringBuilder();
        details.append("Class Name: ").append(classInfo.getClassName()).append("\n");
        details.append("Package: ").append(classInfo.getPackageName()).append("\n");
        details.append("Access Modifier: ").append(classInfo.getAccessModifier()).append("\n");
        details.append("Abstract: ").append(classInfo.isAbstract() ? "Yes" : "No").append("\n");
        details.append("Final: ").append(classInfo.isFinal() ? "Yes" : "No").append("\n");
        details.append("Interface: ").append(classInfo.isInterface() ? "Yes" : "No").append("\n\n");
        
        details.append("Imports:\n");
        for (String importStmt : classInfo.getImports()) {
            details.append("  - ").append(importStmt).append("\n");
        }
        
        classDetailsArea.setText(details.toString());
    }
}