package application.models;

public class GraphEdge {
    
    private final String fromNodeId;
    private final String toNodeId;
    private final String type;
    
    public GraphEdge(String fromNodeId, String toNodeId, String type) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.type = type;
    }
    
    public String getFromNodeId() { return fromNodeId; }
    public String getToNodeId() { return toNodeId; }
    public String getType() { return type; }
}