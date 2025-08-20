package application.services;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import application.models.MethodCallGraph;
import application.models.ClassHierarchyGraph;
import application.models.GraphNode;
import application.models.GraphEdge;

import java.util.*;

public class GraphService {
    
    public MethodCallGraph generateMethodCallGraph(CompilationUnit compilationUnit) {
        if (compilationUnit == null) {
            return createEmptyMethodCallGraph();
        }
        
        MethodCallGraphGenerator generator = new MethodCallGraphGenerator();
        compilationUnit.accept(generator, null);
        
        return generator.getMethodCallGraph();
    }
    
    public ClassHierarchyGraph generateClassHierarchy(CompilationUnit compilationUnit) {
        if (compilationUnit == null) {
            return createEmptyClassHierarchy();
        }
        
        ClassHierarchyGenerator generator = new ClassHierarchyGenerator();
        compilationUnit.accept(generator, null);
        
        return generator.getClassHierarchy();
    }
    
    private MethodCallGraph createEmptyMethodCallGraph() {
        return new MethodCallGraph(new HashMap<>(), new ArrayList<>(), new ArrayList<>());
    }
    
    private ClassHierarchyGraph createEmptyClassHierarchy() {
        return new ClassHierarchyGraph("", "", new ArrayList<>(), new ArrayList<>(), 
                                      new ArrayList<>(), new ArrayList<>());
    }
    
    private class MethodCallGraphGenerator extends VoidVisitorAdapter<Void> {
        
        private Map<String, List<String>> methodCalls = new HashMap<>();
        private List<GraphNode> nodes = new ArrayList<>();
        private List<GraphEdge> edges = new ArrayList<>();
        private Set<String> allMethods = new HashSet<>();
        private String currentMethod = "";
        
        @Override
        public void visit(MethodDeclaration md, Void arg) {
            currentMethod = md.getNameAsString();
            allMethods.add(currentMethod);
            
            if (!methodCalls.containsKey(currentMethod)) {
                methodCalls.put(currentMethod, new ArrayList<>());
            }
            
            super.visit(md, arg);
        }
        
        @Override
        public void visit(MethodCallExpr mce, Void arg) {
            if (!currentMethod.isEmpty()) {
                String calledMethod = mce.getNameAsString();
                
                methodCalls.get(currentMethod).add(calledMethod);
                allMethods.add(calledMethod);
                
                edges.add(new GraphEdge(currentMethod, calledMethod, "calls"));
            }
            
            super.visit(mce, arg);
        }
        
        public MethodCallGraph getMethodCallGraph() {
            for (String method : allMethods) {
                nodes.add(new GraphNode(method, method, "method"));
            }
            
            return new MethodCallGraph(methodCalls, nodes, edges);
        }
    }
    
    private class ClassHierarchyGenerator extends VoidVisitorAdapter<Void> {
        
        private String className = "";
        private String superClass = "";
        private List<String> interfaces = new ArrayList<>();
        private List<String> subClasses = new ArrayList<>();
        private List<GraphNode> nodes = new ArrayList<>();
        private List<GraphEdge> edges = new ArrayList<>();
        
        @Override
        public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
            if (className.isEmpty()) {
                className = cid.getNameAsString();
                
                nodes.add(new GraphNode(className, className, 
                         cid.isInterface() ? "interface" : "class"));
                
                if (!cid.getExtendedTypes().isEmpty()) {
                    ClassOrInterfaceType extendedType = cid.getExtendedTypes().get(0);
                    superClass = extendedType.getNameAsString();
                    
                    nodes.add(new GraphNode(superClass, superClass, "class"));
                    edges.add(new GraphEdge(className, superClass, "extends"));
                }
                
                for (ClassOrInterfaceType implementedType : cid.getImplementedTypes()) {
                    String interfaceName = implementedType.getNameAsString();
                    interfaces.add(interfaceName);
                    
                    nodes.add(new GraphNode(interfaceName, interfaceName, "interface"));
                    edges.add(new GraphEdge(className, interfaceName, "implements"));
                }
            }
            
            super.visit(cid, arg);
        }
        
        public ClassHierarchyGraph getClassHierarchy() {
            return new ClassHierarchyGraph(className, superClass, interfaces, 
                                         subClasses, nodes, edges);
        }
    }
}