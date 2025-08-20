package application.models;

import java.util.List;

public class ClassInfo {
    
    private final String className;
    private final String packageName;
    private final List<FieldInfo> fields;
    private final List<MethodInfo> methods;
    private final List<String> imports;
    private final String accessModifier;
    private final boolean isAbstract;
    private final boolean isFinal;
    private final boolean isInterface;
    
    public ClassInfo(String className, String packageName, List<FieldInfo> fields,
                    List<MethodInfo> methods, List<String> imports, String accessModifier,
                    boolean isAbstract, boolean isFinal, boolean isInterface) {
        this.className = className;
        this.packageName = packageName;
        this.fields = fields;
        this.methods = methods;
        this.imports = imports;
        this.accessModifier = accessModifier;
        this.isAbstract = isAbstract;
        this.isFinal = isFinal;
        this.isInterface = isInterface;
    }
    
    public String getClassName() { return className; }
    public String getPackageName() { return packageName; }
    public List<FieldInfo> getFields() { return fields; }
    public List<MethodInfo> getMethods() { return methods; }
    public List<String> getImports() { return imports; }
    public String getAccessModifier() { return accessModifier; }
    public boolean isAbstract() { return isAbstract; }
    public boolean isFinal() { return isFinal; }
    public boolean isInterface() { return isInterface; }
}