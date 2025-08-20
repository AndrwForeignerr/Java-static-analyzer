package application.services;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import application.models.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AnalyzerService {
    
    private CompilationUnit compilationUnit;
    private JavaParser javaParser;
    
    public AnalyzerService() {
        javaParser = new JavaParser();
    }
    
    public void parseCode(String sourceCode) throws Exception {
        ParseResult<CompilationUnit> parseResult = javaParser.parse(sourceCode);
        
        if (!parseResult.isSuccessful()) {
            throw new RuntimeException("Failed to parse source code: " + parseResult.getProblems());
        }
        
        Optional<CompilationUnit> result = parseResult.getResult();
        if (!result.isPresent()) {
            throw new RuntimeException("No compilation unit found in parsed code");
        }
        
        this.compilationUnit = result.get();
    }
    
    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }
    
    public ClassInfo extractClassInfo() {
        if (compilationUnit == null) {
            throw new IllegalStateException("No compilation unit available. Call parseCode() first.");
        }
        
        ClassInfoExtractor extractor = new ClassInfoExtractor();
        compilationUnit.accept(extractor, null);
        
        return extractor.getClassInfo();
    }
    
    private class ClassInfoExtractor extends VoidVisitorAdapter<Void> {
        
        private String className;
        private String packageName = "";
        private List<FieldInfo> fields = new ArrayList<>();
        private List<MethodInfo> methods = new ArrayList<>();
        private List<String> imports = new ArrayList<>();
        private String accessModifier = "package-private";
        private boolean isAbstract = false;
        private boolean isFinal = false;
        private boolean isInterface = false;
        
        @Override
        public void visit(com.github.javaparser.ast.PackageDeclaration pd, Void arg) {
            packageName = pd.getNameAsString();
            super.visit(pd, arg);
        }
        
        @Override
        public void visit(com.github.javaparser.ast.ImportDeclaration id, Void arg) {
            imports.add(id.getNameAsString());
            super.visit(id, arg);
        }
        
        @Override
        public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
            if (className == null) {
                className = cid.getNameAsString();
                isInterface = cid.isInterface();
                isAbstract = cid.isAbstract();
                isFinal = cid.isFinal();
                
                if (cid.isPublic()) accessModifier = "public";
                else if (cid.isPrivate()) accessModifier = "private";
                else if (cid.isProtected()) accessModifier = "protected";
            }
            super.visit(cid, arg);
        }
        
        @Override
        public void visit(FieldDeclaration fd, Void arg) {
            final String fieldAccessModifier;
            if (fd.isPublic()) fieldAccessModifier = "public";
            else if (fd.isPrivate()) fieldAccessModifier = "private";
            else if (fd.isProtected()) fieldAccessModifier = "protected";
            else fieldAccessModifier = "package-private";
            
            final String fieldType = fd.getElementType().asString();
            final boolean fieldIsStatic = fd.isStatic();
            final boolean fieldIsFinal = fd.isFinal();
            
            fd.getVariables().forEach(variable -> {
                String fieldName = variable.getNameAsString();
                fields.add(new FieldInfo(fieldName, fieldType, fieldAccessModifier, 
                                       fieldIsStatic, fieldIsFinal));
            });
            
            super.visit(fd, arg);
        }
        
        @Override
        public void visit(MethodDeclaration md, Void arg) {
            String methodName = md.getNameAsString();
            String returnType = md.getTypeAsString();
            
            final String methodAccessModifier;
            if (md.isPublic()) methodAccessModifier = "public";
            else if (md.isPrivate()) methodAccessModifier = "private";
            else if (md.isProtected()) methodAccessModifier = "protected";
            else methodAccessModifier = "package-private";
            
            final boolean methodIsStatic = md.isStatic();
            final boolean methodIsAbstract = md.isAbstract();
            final boolean methodIsFinal = md.isFinal();
            
            List<String> parameters = md.getParameters().stream()
                    .map(param -> param.getType().asString() + " " + param.getNameAsString())
                    .collect(Collectors.toList());
            
            methods.add(new MethodInfo(methodName, returnType, parameters, 
                                     methodAccessModifier, methodIsStatic, 
                                     methodIsAbstract, methodIsFinal));
            
            super.visit(md, arg);
        }
        
        @Override
        public void visit(ConstructorDeclaration cd, Void arg) {
            String constructorName = cd.getNameAsString();
            String returnType = "void";
            
            final String constructorAccessModifier;
            if (cd.isPublic()) constructorAccessModifier = "public";
            else if (cd.isPrivate()) constructorAccessModifier = "private";
            else if (cd.isProtected()) constructorAccessModifier = "protected";
            else constructorAccessModifier = "package-private";
            
            List<String> parameters = cd.getParameters().stream()
                    .map(param -> param.getType().asString() + " " + param.getNameAsString())
                    .collect(Collectors.toList());
            
            methods.add(new MethodInfo(constructorName, returnType, parameters, 
                                     constructorAccessModifier, false, false, false));
            
            super.visit(cd, arg);
        }
        
        public ClassInfo getClassInfo() {
            return new ClassInfo(className, packageName, fields, methods, imports, 
                               accessModifier, isAbstract, isFinal, isInterface);
        }
    }
}