package application.ui.components;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Affine;
import application.models.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class GraphVisualizationPanel extends VBox {
    
    private TabPane graphTabPane;
    private Canvas methodCallCanvas;
    private Canvas hierarchyCanvas;
    private Slider spacingSlider;
    private ComboBox<String> layoutComboBox;
    private Button resetViewButton;
    private Button fitToScreenButton;
    private Label zoomLabel;
    
    // Pan and zoom state
    private double zoomLevel = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private double lastMouseX;
    private double lastMouseY;
    private boolean isPanning = false;
    
    // Canvas size for large graphs
    private static final double CANVAS_WIDTH = 3000;
    private static final double CANVAS_HEIGHT = 2000;
    
    // Selection tracking
    private GraphNode selectedMethodNode = null;
    private GraphNode selectedClassNode = null;
    private Map<String, NodePosition> currentNodePositions = new HashMap<>();
    
    // Current graph data
    private MethodCallGraph currentMethodGraph;
    private ClassHierarchyGraph currentHierarchyGraph;
    
    public GraphVisualizationPanel() {
        initializeComponents();
        setupLayout();
        setupCanvasInteraction();
    }
    
    private void initializeComponents() {
        graphTabPane = new TabPane();
        
        // Create larger canvases for big graphs
        methodCallCanvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        hierarchyCanvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        
        spacingSlider = new Slider(50, 200, 120);
        spacingSlider.setMajorTickUnit(50);
        spacingSlider.setShowTickLabels(true);
        
        layoutComboBox = new ComboBox<>();
        layoutComboBox.getItems().addAll("Hierarchical", "Circular", "Grid");
        layoutComboBox.setValue("Hierarchical");
        
        resetViewButton = new Button("Reset View");
        fitToScreenButton = new Button("Fit to Screen");
        zoomLabel = new Label("Zoom: 100%");
    }
    
    private void setupLayout() {
        Label titleLabel = new Label("Graph Visualization");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        HBox controlsBox = new HBox(15);
        controlsBox.setPadding(new Insets(10));
        controlsBox.getChildren().addAll(
            new Label("Layout:"), layoutComboBox,
            new Label("Spacing:"), spacingSlider,
            new Separator(),
            resetViewButton,
            fitToScreenButton,
            zoomLabel
        );
        
        Tab methodCallTab = new Tab("Method Call Graph");
        ScrollPane methodScrollPane = createScrollPaneForCanvas(methodCallCanvas);
        VBox methodContent = new VBox();
        methodContent.getChildren().addAll(controlsBox, methodScrollPane);
        methodCallTab.setContent(methodContent);
        methodCallTab.setClosable(false);
        
        Tab hierarchyTab = new Tab("Class Hierarchy");
        ScrollPane hierarchyScrollPane = createScrollPaneForCanvas(hierarchyCanvas);
        hierarchyTab.setContent(hierarchyScrollPane);
        hierarchyTab.setClosable(false);
        
        graphTabPane.getTabs().addAll(methodCallTab, hierarchyTab);
        
        // Event handlers
        spacingSlider.valueProperty().addListener((obs, oldVal, newVal) -> redrawGraphs());
        layoutComboBox.valueProperty().addListener((obs, oldVal, newVal) -> redrawGraphs());
        
        resetViewButton.setOnAction(e -> resetView());
        fitToScreenButton.setOnAction(e -> fitToScreen());
        
        this.setPadding(new Insets(10));
        this.getChildren().addAll(titleLabel, graphTabPane);
        VBox.setVgrow(graphTabPane, Priority.ALWAYS);
    }
    
    private ScrollPane createScrollPaneForCanvas(Canvas canvas) {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(canvas);
        scrollPane.setPannable(false); // We'll handle panning ourselves
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return scrollPane;
    }
    
    private void setupCanvasInteraction() {
        setupCanvasInteraction(methodCallCanvas, true);
        setupCanvasInteraction(hierarchyCanvas, false);
    }
    
    private void setupCanvasInteraction(Canvas canvas, boolean isMethodGraph) {
        // Mouse pressed - start panning or select node
        canvas.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                // Try to select a node first
                GraphNode clickedNode = getNodeAt(event.getX(), event.getY(), isMethodGraph);
                
                if (clickedNode != null) {
                    if (isMethodGraph) {
                        selectedMethodNode = clickedNode;
                        drawMethodCallGraph(currentMethodGraph);
                    } else {
                        selectedClassNode = clickedNode;
                        drawClassHierarchy(currentHierarchyGraph);
                    }
                } else {
                    // Start panning
                    isPanning = true;
                    lastMouseX = event.getX();
                    lastMouseY = event.getY();
                    canvas.setCursor(javafx.scene.Cursor.CLOSED_HAND);
                }
            } else if (event.getButton() == MouseButton.SECONDARY) {
                // Right click - clear selection
                if (isMethodGraph) {
                    selectedMethodNode = null;
                    drawMethodCallGraph(currentMethodGraph);
                } else {
                    selectedClassNode = null;
                    drawClassHierarchy(currentHierarchyGraph);
                }
            }
        });
        
        // Mouse dragged - pan the view
        canvas.setOnMouseDragged(event -> {
            if (isPanning) {
                double deltaX = event.getX() - lastMouseX;
                double deltaY = event.getY() - lastMouseY;
                
                offsetX += deltaX;
                offsetY += deltaY;
                
                lastMouseX = event.getX();
                lastMouseY = event.getY();
                
                if (isMethodGraph) {
                    drawMethodCallGraph(currentMethodGraph);
                } else {
                    drawClassHierarchy(currentHierarchyGraph);
                }
            }
        });
        
        // Mouse released - stop panning
        canvas.setOnMouseReleased(event -> {
            isPanning = false;
            canvas.setCursor(javafx.scene.Cursor.DEFAULT);
        });
        
        // Mouse scroll - zoom
        canvas.setOnScroll(event -> handleZoom(event, isMethodGraph));
    }
    
    private void handleZoom(ScrollEvent event, boolean isMethodGraph) {
        double zoomFactor = 1.05;
        double deltaY = event.getDeltaY();
        
        if (deltaY < 0) {
            zoomLevel /= zoomFactor;
        } else {
            zoomLevel *= zoomFactor;
        }
        
        // Limit zoom level
        zoomLevel = Math.max(0.1, Math.min(5.0, zoomLevel));
        
        // Update zoom label
        zoomLabel.setText(String.format("Zoom: %.0f%%", zoomLevel * 100));
        
        // Zoom towards mouse position
        double mouseX = event.getX();
        double mouseY = event.getY();
        
        if (deltaY < 0) {
            offsetX -= (mouseX - offsetX) * (1 - 1/zoomFactor);
            offsetY -= (mouseY - offsetY) * (1 - 1/zoomFactor);
        } else {
            offsetX -= (mouseX - offsetX) * (1 - zoomFactor);
            offsetY -= (mouseY - offsetY) * (1 - zoomFactor);
        }
        
        // Redraw
        if (isMethodGraph) {
            drawMethodCallGraph(currentMethodGraph);
        } else {
            drawClassHierarchy(currentHierarchyGraph);
        }
    }
    
    private void resetView() {
        zoomLevel = 1.0;
        offsetX = 0;
        offsetY = 0;
        zoomLabel.setText("Zoom: 100%");
        redrawGraphs();
    }
    
    private void fitToScreen() {
        if (currentNodePositions.isEmpty()) return;
        
        // Find bounds of all nodes
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        
        for (NodePosition pos : currentNodePositions.values()) {
            minX = Math.min(minX, pos.x - 100);
            minY = Math.min(minY, pos.y - 50);
            maxX = Math.max(maxX, pos.x + 100);
            maxY = Math.max(maxY, pos.y + 50);
        }
        
        double graphWidth = maxX - minX;
        double graphHeight = maxY - minY;
        
        // Calculate zoom to fit
        double canvasWidth = methodCallCanvas.getWidth();
        double canvasHeight = methodCallCanvas.getHeight();
        
        double zoomX = canvasWidth / graphWidth * 0.9;
        double zoomY = canvasHeight / graphHeight * 0.9;
        zoomLevel = Math.min(zoomX, zoomY);
        zoomLevel = Math.max(0.1, Math.min(2.0, zoomLevel));
        
        // Center the graph
        offsetX = (canvasWidth - graphWidth * zoomLevel) / 2 - minX * zoomLevel;
        offsetY = (canvasHeight - graphHeight * zoomLevel) / 2 - minY * zoomLevel;
        
        zoomLabel.setText(String.format("Zoom: %.0f%%", zoomLevel * 100));
        redrawGraphs();
    }
    
    private GraphNode getNodeAt(double x, double y, boolean isMethodGraph) {
        // Adjust coordinates for pan and zoom
        double worldX = (x - offsetX) / zoomLevel;
        double worldY = (y - offsetY) / zoomLevel;
        
        List<GraphNode> nodes;
        Map<String, NodePosition> positions;
        
        if (isMethodGraph && currentMethodGraph != null) {
            nodes = currentMethodGraph.getNodes();
            positions = currentNodePositions;
        } else if (!isMethodGraph && currentHierarchyGraph != null) {
            nodes = currentHierarchyGraph.getNodes();
            positions = currentNodePositions;
        } else {
            return null;
        }
        
        for (GraphNode node : nodes) {
            NodePosition pos = positions.get(node.getId());
            if (pos != null) {
                double nodeWidth = Math.max(100, node.getLabel().length() * 10);
                double nodeHeight = isMethodGraph ? 40 : 60;
                
                if (worldX >= pos.x - nodeWidth/2 && worldX <= pos.x + nodeWidth/2 &&
                    worldY >= pos.y - nodeHeight/2 && worldY <= pos.y + nodeHeight/2) {
                    return node;
                }
            }
        }
        
        return null;
    }
    
    public void displayGraphs(MethodCallGraph methodCallGraph, ClassHierarchyGraph classHierarchy) {
        this.currentMethodGraph = methodCallGraph;
        this.currentHierarchyGraph = classHierarchy;
        redrawGraphs();
    }
    
    private void redrawGraphs() {
        if (currentMethodGraph != null) {
            drawMethodCallGraph(currentMethodGraph);
        }
        if (currentHierarchyGraph != null) {
            drawClassHierarchy(currentHierarchyGraph);
        }
    }
    
    private void drawMethodCallGraph(MethodCallGraph methodCallGraph) {
        GraphicsContext gc = methodCallCanvas.getGraphicsContext2D();
        gc.setTransform(new Affine());
        gc.clearRect(0, 0, methodCallCanvas.getWidth(), methodCallCanvas.getHeight());
        
        if (methodCallGraph == null || methodCallGraph.getNodes().isEmpty()) {
            drawNoDataMessage(gc, "No method call data available");
            return;
        }
        
        // Apply transformations
        gc.save();
        gc.translate(offsetX, offsetY);
        gc.scale(zoomLevel, zoomLevel);
        
        String layout = layoutComboBox.getValue();
        Map<String, NodePosition> nodePositions;
        
        switch (layout) {
            case "Circular":
                nodePositions = calculateCircularLayout(methodCallGraph.getNodes());
                break;
            case "Grid":
                nodePositions = calculateGridLayout(methodCallGraph.getNodes());
                break;
            default:
                nodePositions = calculateHierarchicalLayout(methodCallGraph);
                break;
        }
        
        currentNodePositions = nodePositions;
        
        // Draw edges with correct highlighting logic
        drawMethodEdges(gc, methodCallGraph.getEdges(), nodePositions);
        drawMethodNodes(gc, methodCallGraph.getNodes(), nodePositions);
        
        gc.restore();
    }
    
    private void drawMethodEdges(GraphicsContext gc, List<GraphEdge> edges, 
                                Map<String, NodePosition> positions) {
        
        // Group edges by their relationship to the selected node
        List<GraphEdge> selectedOutgoingEdges = new ArrayList<>();
        List<GraphEdge> selectedIncomingEdges = new ArrayList<>();
        List<GraphEdge> otherEdges = new ArrayList<>();
        
        for (GraphEdge edge : edges) {
            if (selectedMethodNode != null) {
                if (edge.getFromNodeId().equals(selectedMethodNode.getId())) {
                    selectedOutgoingEdges.add(edge);
                } else if (edge.getToNodeId().equals(selectedMethodNode.getId())) {
                    selectedIncomingEdges.add(edge);
                } else {
                    otherEdges.add(edge);
                }
            } else {
                otherEdges.add(edge);
            }
        }
        
        // Draw edges in layers for proper visibility
        
        // 1. Draw unrelated edges (very light)
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1);
        for (GraphEdge edge : otherEdges) {
            NodePosition from = positions.get(edge.getFromNodeId());
            NodePosition to = positions.get(edge.getToNodeId());
            if (from != null && to != null) {
                drawArrow(gc, from.x, from.y, to.x, to.y);
            }
        }
        
        // 2. Draw incoming edges to selected node (medium visibility)
        if (!selectedIncomingEdges.isEmpty()) {
            gc.setStroke(Color.DARKGRAY);
            gc.setLineWidth(2);
            for (GraphEdge edge : selectedIncomingEdges) {
                NodePosition from = positions.get(edge.getFromNodeId());
                NodePosition to = positions.get(edge.getToNodeId());
                if (from != null && to != null) {
                    drawArrow(gc, from.x, from.y, to.x, to.y);
                }
            }
        }
        
        // 3. Draw outgoing edges from selected node (highlighted in blue)
        if (!selectedOutgoingEdges.isEmpty()) {
            gc.setStroke(Color.BLUE);
            gc.setLineWidth(3);
            for (GraphEdge edge : selectedOutgoingEdges) {
                NodePosition from = positions.get(edge.getFromNodeId());
                NodePosition to = positions.get(edge.getToNodeId());
                if (from != null && to != null) {
                    drawArrow(gc, from.x, from.y, to.x, to.y);
                }
            }
        }
    }
    
    private void drawClassHierarchy(ClassHierarchyGraph classHierarchy) {
        GraphicsContext gc = hierarchyCanvas.getGraphicsContext2D();
        gc.setTransform(new Affine());
        gc.clearRect(0, 0, hierarchyCanvas.getWidth(), hierarchyCanvas.getHeight());
        
        if (classHierarchy == null || classHierarchy.getNodes().isEmpty()) {
            drawNoDataMessage(gc, "No class hierarchy data available");
            return;
        }
        
        // Apply transformations
        gc.save();
        gc.translate(offsetX, offsetY);
        gc.scale(zoomLevel, zoomLevel);
        
        Map<String, NodePosition> nodePositions = calculateClassHierarchyLayout(classHierarchy);
        currentNodePositions = nodePositions;
        
        drawClassEdges(gc, classHierarchy.getEdges(), nodePositions);
        drawClassNodes(gc, classHierarchy.getNodes(), nodePositions);
        
        gc.restore();
    }
    
    private void drawClassEdges(GraphicsContext gc, List<GraphEdge> edges,
                               Map<String, NodePosition> positions) {
        
        for (GraphEdge edge : edges) {
            NodePosition from = positions.get(edge.getFromNodeId());
            NodePosition to = positions.get(edge.getToNodeId());
            
            if (from != null && to != null) {
                // Determine edge style based on type and selection
                if (selectedClassNode != null &&
                    (edge.getFromNodeId().equals(selectedClassNode.getId()) ||
                     edge.getToNodeId().equals(selectedClassNode.getId()))) {
                    gc.setStroke(Color.DARKGREEN);
                    gc.setLineWidth(3);
                } else {
                    gc.setStroke(Color.GRAY);
                    gc.setLineWidth(1);
                }
                
                drawArrow(gc, from.x, from.y, to.x, to.y);
            }
        }
    }
    
    private Map<String, NodePosition> calculateHierarchicalLayout(MethodCallGraph graph) {
        Map<String, NodePosition> positions = new HashMap<>();
        List<GraphNode> nodes = graph.getNodes();
        
        Map<String, Set<String>> callers = new HashMap<>();
        Map<String, Set<String>> callees = new HashMap<>();
        
        for (GraphEdge edge : graph.getEdges()) {
            callers.computeIfAbsent(edge.getToNodeId(), k -> new HashSet<>()).add(edge.getFromNodeId());
            callees.computeIfAbsent(edge.getFromNodeId(), k -> new HashSet<>()).add(edge.getToNodeId());
        }
        
        List<List<String>> levels = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        
        List<String> rootNodes = new ArrayList<>();
        for (GraphNode node : nodes) {
            if (!callers.containsKey(node.getId()) || callers.get(node.getId()).isEmpty()) {
                rootNodes.add(node.getId());
            }
        }
        
        if (rootNodes.isEmpty()) {
            rootNodes.add(nodes.get(0).getId());
        }
        
        levels.add(new ArrayList<>(rootNodes));
        visited.addAll(rootNodes);
        
        while (visited.size() < nodes.size()) {
            List<String> nextLevel = new ArrayList<>();
            for (String nodeId : levels.get(levels.size() - 1)) {
                if (callees.containsKey(nodeId)) {
                    for (String callee : callees.get(nodeId)) {
                        if (!visited.contains(callee)) {
                            nextLevel.add(callee);
                            visited.add(callee);
                        }
                    }
                }
            }
            
            if (nextLevel.isEmpty()) {
                for (GraphNode node : nodes) {
                    if (!visited.contains(node.getId())) {
                        nextLevel.add(node.getId());
                        visited.add(node.getId());
                        break;
                    }
                }
            }
            
            if (!nextLevel.isEmpty()) {
                levels.add(nextLevel);
            } else {
                break;
            }
        }
        
        double spacing = spacingSlider.getValue();
        double startX = CANVAS_WIDTH / 2;
        double startY = 100;
        
        for (int levelIndex = 0; levelIndex < levels.size(); levelIndex++) {
            List<String> level = levels.get(levelIndex);
            double levelWidth = level.size() * spacing;
            double levelStartX = startX - levelWidth / 2;
            
            for (int nodeIndex = 0; nodeIndex < level.size(); nodeIndex++) {
                String nodeId = level.get(nodeIndex);
                double x = levelStartX + nodeIndex * spacing;
                double y = startY + levelIndex * spacing;
                positions.put(nodeId, new NodePosition(x, y));
            }
        }
        
        return positions;
    }
    
    private Map<String, NodePosition> calculateCircularLayout(List<GraphNode> nodes) {
        Map<String, NodePosition> positions = new HashMap<>();
        
        double centerX = CANVAS_WIDTH / 2;
        double centerY = CANVAS_HEIGHT / 2;
        double radius = Math.min(centerX, centerY) - 200;
        
        for (int i = 0; i < nodes.size(); i++) {
            double angle = 2 * Math.PI * i / nodes.size();
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            positions.put(nodes.get(i).getId(), new NodePosition(x, y));
        }
        
        return positions;
    }
    
    private Map<String, NodePosition> calculateGridLayout(List<GraphNode> nodes) {
        Map<String, NodePosition> positions = new HashMap<>();
        
        int cols = (int) Math.ceil(Math.sqrt(nodes.size()));
        int rows = (int) Math.ceil((double) nodes.size() / cols);
        
        double spacing = spacingSlider.getValue();
        double totalWidth = cols * spacing;
        double totalHeight = rows * spacing;
        
        double startX = (CANVAS_WIDTH - totalWidth) / 2;
        double startY = (CANVAS_HEIGHT - totalHeight) / 2;
        
        for (int i = 0; i < nodes.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            
            double x = startX + col * spacing;
            double y = startY + row * spacing;
            
            positions.put(nodes.get(i).getId(), new NodePosition(x, y));
        }
        
        return positions;
    }
    
    private Map<String, NodePosition> calculateClassHierarchyLayout(ClassHierarchyGraph hierarchy) {
        Map<String, NodePosition> positions = new HashMap<>();
        List<GraphNode> nodes = hierarchy.getNodes();
        
        double centerX = CANVAS_WIDTH / 2;
        double startY = 100;
        double levelHeight = 150;
        
        Map<String, Integer> levels = new HashMap<>();
        
        for (GraphNode node : nodes) {
            if ("class".equals(node.getType()) && isRootClass(node, hierarchy.getEdges())) {
                levels.put(node.getId(), 0);
            }
        }
        
        if (levels.isEmpty() && !nodes.isEmpty()) {
            levels.put(nodes.get(0).getId(), 0);
        }
        
        for (GraphEdge edge : hierarchy.getEdges()) {
            if ("extends".equals(edge.getType())) {
                int parentLevel = levels.getOrDefault(edge.getToNodeId(), 0);
                levels.put(edge.getFromNodeId(), parentLevel + 1);
            }
        }
        
        for (GraphNode node : nodes) {
            if (!levels.containsKey(node.getId())) {
                levels.put(node.getId(), "interface".equals(node.getType()) ? 0 : 1);
            }
        }
        
        Map<Integer, List<String>> levelGroups = new HashMap<>();
        for (Map.Entry<String, Integer> entry : levels.entrySet()) {
            levelGroups.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }
        
        for (Map.Entry<Integer, List<String>> levelGroup : levelGroups.entrySet()) {
            int level = levelGroup.getKey();
            List<String> nodesInLevel = levelGroup.getValue();
            
            double levelY = startY + level * levelHeight;
            double totalWidth = nodesInLevel.size() * 200;
            double startX = centerX - totalWidth / 2;
            
            for (int i = 0; i < nodesInLevel.size(); i++) {
                double x = startX + i * 200;
                positions.put(nodesInLevel.get(i), new NodePosition(x, levelY));
            }
        }
        
        return positions;
    }
    
    private boolean isRootClass(GraphNode node, List<GraphEdge> edges) {
        for (GraphEdge edge : edges) {
            if (edge.getFromNodeId().equals(node.getId()) && "extends".equals(edge.getType())) {
                return false;
            }
        }
        return true;
    }
    
    private void drawMethodNodes(GraphicsContext gc, List<GraphNode> nodes, 
                                Map<String, NodePosition> positions) {
        for (GraphNode node : nodes) {
            NodePosition pos = positions.get(node.getId());
            if (pos != null) {
                boolean isSelected = selectedMethodNode != null && 
                                   selectedMethodNode.getId().equals(node.getId());
                
                // Check if this node is called by the selected node
                boolean isCalledBySelected = false;
                if (selectedMethodNode != null && currentMethodGraph != null) {
                    isCalledBySelected = currentMethodGraph.getEdges().stream()
                        .anyMatch(edge -> edge.getFromNodeId().equals(selectedMethodNode.getId()) &&
                                        edge.getToNodeId().equals(node.getId()));
                }
                
                drawMethodNode(gc, node.getLabel(), pos.x, pos.y, isSelected, isCalledBySelected);
            }
        }
    }
    
    private void drawClassNodes(GraphicsContext gc, List<GraphNode> nodes, 
                               Map<String, NodePosition> positions) {
        for (GraphNode node : nodes) {
            NodePosition pos = positions.get(node.getId());
            if (pos != null) {
                boolean isSelected = selectedClassNode != null && 
                                   selectedClassNode.getId().equals(node.getId());
                drawClassNode(gc, node.getLabel(), pos.x, pos.y, node.getType(), isSelected);
            }
        }
    }
    
    private void drawMethodNode(GraphicsContext gc, String methodName, double x, double y, 
                               boolean isSelected, boolean isCalledBySelected) {
        double nodeWidth = Math.max(80, methodName.length() * 8);
        double nodeHeight = 40;
        
        // Fill color based on state
        if (isSelected) {
            gc.setFill(Color.LIGHTCORAL);
        } else if (isCalledBySelected) {
            gc.setFill(Color.LIGHTBLUE);
        } else {
            gc.setFill(Color.LIGHTGREEN);
        }
        
        gc.fillRoundRect(x - nodeWidth/2, y - nodeHeight/2, nodeWidth, nodeHeight, 10, 10);
        
        // Border - using original color scheme
        gc.setStroke(isSelected ? Color.DARKRED : Color.DARKBLUE);
        gc.setLineWidth(isSelected ? 3 : 2);
        gc.strokeRoundRect(x - nodeWidth/2, y - nodeHeight/2, nodeWidth, nodeHeight, 10, 10);
        
        // Text - back to original font
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Courier New", 12));
        
        String displayName = methodName.length() > 15 ? methodName.substring(0, 12) + "..." : methodName;
        double textWidth = displayName.length() * 6;
        gc.fillText(displayName, x - textWidth/2, y + 3);
    }
    
    private void drawClassNode(GraphicsContext gc, String className, double x, double y, 
                              String type, boolean isSelected) {
        double nodeWidth = Math.max(100, className.length() * 10);
        double nodeHeight = 60;
        
        Color fillColor = "interface".equals(type) ? 
            (isSelected ? Color.DARKSEAGREEN : Color.LIGHTGREEN) : 
            (isSelected ? Color.LIGHTSALMON : Color.LIGHTYELLOW);
        Color strokeColor = "interface".equals(type) ? Color.DARKGREEN : Color.DARKORANGE;
        
        gc.setFill(fillColor);
        gc.fillRect(x - nodeWidth/2, y - nodeHeight/2, nodeWidth, nodeHeight);
        
        gc.setStroke(strokeColor);
        gc.setLineWidth(isSelected ? 3 : 2);
        gc.strokeRect(x - nodeWidth/2, y - nodeHeight/2, nodeWidth, nodeHeight);
        
        // Text - back to original font
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
        
        String displayName = className.length() > 12 ? className.substring(0, 9) + "..." : className;
        double textWidth = displayName.length() * 6;
        gc.fillText(displayName, x - textWidth/2, y - 5);
        
        if (type != null) {
            gc.setFont(Font.font("Courier New", 10));
            String typeText = "<<" + type + ">>";
            double typeWidth = typeText.length() * 4;
            gc.fillText(typeText, x - typeWidth/2, y + 10);
        }
    }
    
    private void drawArrow(GraphicsContext gc, double x1, double y1, double x2, double y2) {
        gc.strokeLine(x1, y1, x2, y2);
        
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double arrowLength = 10;
        double arrowAngle = Math.PI / 6;
        
        double x3 = x2 - arrowLength * Math.cos(angle - arrowAngle);
        double y3 = y2 - arrowLength * Math.sin(angle - arrowAngle);
        
        double x4 = x2 - arrowLength * Math.cos(angle + arrowAngle);
        double y4 = y2 - arrowLength * Math.sin(angle + arrowAngle);
        
        gc.strokeLine(x2, y2, x3, y3);
        gc.strokeLine(x2, y2, x4, y4);
    }
    
    private void drawNoDataMessage(GraphicsContext gc, String message) {
        gc.setFill(Color.GRAY);
        gc.setFont(Font.font("Courier New", 16));
        gc.fillText(message, 50, 50);
    }
    
    private static class NodePosition {
        final double x, y;
        
        NodePosition(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}