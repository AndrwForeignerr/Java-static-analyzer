package application.models;

import java.util.List;

public class MethodInfo {
    
    private final String name;
    private final String returnType;
    private final List<String> parameters;
    private final String accessModifier;
    private final boolean isStatic;
    private final boolean isAbstract;
    private final boolean isFinal;
    
    public MethodInfo(String name, String returnType, List<String> parameters,
                     String accessModifier, boolean isStatic, boolean isAbstract, 
                     boolean isFinal) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.accessModifier = accessModifier;
        this.isStatic = isStatic;
        this.isAbstract = isAbstract;
        this.isFinal = isFinal;
    }
    
    public String getName() { return name; }
    public String getReturnType() { return returnType; }
    public List<String> getParameters() { return parameters; }
    public String getAccessModifier() { return accessModifier; }
    public boolean isStatic() { return isStatic; }
    public boolean isAbstract() { return isAbstract; }
    public boolean isFinal() { return isFinal; }
}