package application.models;

public class FieldInfo {
    
    private final String name;
    private final String type;
    private final String accessModifier;
    private final boolean isStatic;
    private final boolean isFinal;
    
    public FieldInfo(String name, String type, String accessModifier, 
                    boolean isStatic, boolean isFinal) {
        this.name = name;
        this.type = type;
        this.accessModifier = accessModifier;
        this.isStatic = isStatic;
        this.isFinal = isFinal;
    }
    
    public String getName() { return name; }
    public String getType() { return type; }
    public String getAccessModifier() { return accessModifier; }
    public boolean isStatic() { return isStatic; }
    public boolean isFinal() { return isFinal; }
}