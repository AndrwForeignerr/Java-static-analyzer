package application.models;

import java.util.List;

public class ClassHierarchyGraph {
    
    private final String className;
    private final String superClass;
    private final List<String> interfaces;
    private final List<String> subClasses;
    private final List<GraphNode> nodes;
    private final List<GraphEdge> edges;
    
    public ClassHierarchyGraph(String className, String superClass, 
                              List<String> interfaces, List<String> subClasses,
                              List<GraphNode> nodes, List<GraphEdge> edges) {
        this.className = className;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.subClasses = subClasses;
        this.nodes = nodes;
        this.edges = edges;
    }
    
    public String getClassName() { return className; }
    public String getSuperClass() { return superClass; }
    public List<String> getInterfaces() { return interfaces; }
    public List<String> getSubClasses() { return subClasses; }
    public List<GraphNode> getNodes() { return nodes; }
    public List<GraphEdge> getEdges() { return edges; }
}
	