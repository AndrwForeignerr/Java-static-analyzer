
package application.models;

import java.util.List;
import java.util.Map;

public class MethodCallGraph {
    
    private final Map<String, List<String>> methodCalls;
    private final List<GraphNode> nodes;
    private final List<GraphEdge> edges;
    
    public MethodCallGraph(Map<String, List<String>> methodCalls, 
                          List<GraphNode> nodes, List<GraphEdge> edges) {
        this.methodCalls = methodCalls;
        this.nodes = nodes;
        this.edges = edges;
    }
    
    public Map<String, List<String>> getMethodCalls() { return methodCalls; }
    public List<GraphNode> getNodes() { return nodes; }
    public List<GraphEdge> getEdges() { return edges; }
}