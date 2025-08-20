package application.services;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import application.models.OptimizationSuggestion;
import application.models.OptimizationSeverity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.regex.Pattern;

public class OptimizationService {
    
    private final CSGenerator codeGenerator = new CSGenerator();
    
    public List<OptimizationSuggestion> analyzeOptimizations(CompilationUnit compilationUnit) {
        List<OptimizationSuggestion> suggestions = new ArrayList<>();
        
        OptimizationAnalyzer analyzer = new OptimizationAnalyzer(suggestions);
        compilationUnit.accept(analyzer, null);
        
        return suggestions;
    }
    
    private class OptimizationAnalyzer extends VoidVisitorAdapter<Void> {
        
        private final List<OptimizationSuggestion> suggestions;
        private final Set<String> usedVariables = new HashSet<>();
        private final Set<String> declaredVariables = new HashSet<>();
        private final Set<String> fieldNames = new HashSet<>();
        private final Set<String> assignedVariables = new HashSet<>();
        private CompilationUnit currentCompilationUnit;
        private MethodDeclaration currentMethod;
        
        public OptimizationAnalyzer(List<OptimizationSuggestion> suggestions) {
            this.suggestions = suggestions;
        }
        
        @Override
        public void visit(CompilationUnit cu, Void arg) {
            this.currentCompilationUnit = cu;
            collectFieldNames(cu);
            super.visit(cu, arg);
        }
        
        private void collectFieldNames(CompilationUnit cu) {
            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(FieldDeclaration fd, Void arg) {
                    fd.getVariables().forEach(var -> fieldNames.add(var.getNameAsString()));
                    super.visit(fd, arg);
                }
            }, null);
        }
        
        @Override
        public void visit(MethodDeclaration md, Void arg) {
            this.currentMethod = md;
            usedVariables.clear();
            declaredVariables.clear();
            assignedVariables.clear();
            
            collectAllVariableUsage(md);
            
            super.visit(md, arg);
            
            checkMethodOptimization(md);
            checkCyclomaticComplexity(md);
        }
        
        private void collectAllVariableUsage(Node node) {
            node.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(VariableDeclarator vd, Void arg) {
                    String varName = vd.getNameAsString();
                    declaredVariables.add(varName);
                    
                    if (vd.getInitializer().isPresent()) {
                        Expression init = vd.getInitializer().get();
                        if (init instanceof NameExpr) {
                            usedVariables.add(((NameExpr) init).getNameAsString());
                        }
                        collectVariableReferencesInExpression(init);
                    }
                    super.visit(vd, arg);
                }
                
                @Override
                public void visit(NameExpr ne, Void arg) {
                    String varName = ne.getNameAsString();
                    usedVariables.add(varName);
                    super.visit(ne, arg);
                }
                
                @Override
                public void visit(FieldAccessExpr fae, Void arg) {
                    if (fae.getScope() instanceof ThisExpr) {
                        usedVariables.add(fae.getNameAsString());
                    }
                    super.visit(fae, arg);
                }
                
                @Override
                public void visit(AssignExpr ae, Void arg) {
                    if (ae.getTarget() instanceof NameExpr) {
                        String varName = ((NameExpr) ae.getTarget()).getNameAsString();
                        usedVariables.add(varName);
                        assignedVariables.add(varName);
                    }
                    if (ae.getTarget() instanceof FieldAccessExpr) {
                        FieldAccessExpr fae = (FieldAccessExpr) ae.getTarget();
                        if (fae.getScope() instanceof ThisExpr) {
                            usedVariables.add(fae.getNameAsString());
                        }
                    }
                    
                    collectVariableReferencesInExpression(ae.getValue());
                    super.visit(ae, arg);
                }
                
                @Override
                public void visit(UnaryExpr ue, Void arg) {
                    if (ue.getExpression() instanceof NameExpr) {
                        String varName = ((NameExpr) ue.getExpression()).getNameAsString();
                        usedVariables.add(varName);
                        if (ue.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT ||
                            ue.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT ||
                            ue.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT ||
                            ue.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT) {
                            assignedVariables.add(varName);
                        }
                    }
                    super.visit(ue, arg);
                }
                
                @Override
                public void visit(MethodCallExpr mce, Void arg) {
                    for (Expression argExpr : mce.getArguments()) {
                        collectVariableReferencesInExpression(argExpr);
                    }
                    super.visit(mce, arg);
                }
                
                @Override
                public void visit(ReturnStmt rs, Void arg) {
                    if (rs.getExpression().isPresent()) {
                        collectVariableReferencesInExpression(rs.getExpression().get());
                    }
                    super.visit(rs, arg);
                }
                
                @Override
                public void visit(IfStmt is, Void arg) {
                    collectVariableReferencesInExpression(is.getCondition());
                    super.visit(is, arg);
                }
                
                @Override
                public void visit(WhileStmt ws, Void arg) {
                    collectVariableReferencesInExpression(ws.getCondition());
                    super.visit(ws, arg);
                }
                
                @Override
                public void visit(ForStmt fs, Void arg) {
                    if (fs.getCompare().isPresent()) {
                        collectVariableReferencesInExpression(fs.getCompare().get());
                    }
                    super.visit(fs, arg);
                }
            }, null);
        }
        
        private void collectVariableReferencesInExpression(Expression expr) {
            if (expr instanceof NameExpr) {
                usedVariables.add(((NameExpr) expr).getNameAsString());
            } else if (expr instanceof BinaryExpr) {
                BinaryExpr be = (BinaryExpr) expr;
                collectVariableReferencesInExpression(be.getLeft());
                collectVariableReferencesInExpression(be.getRight());
            } else if (expr instanceof MethodCallExpr) {
                MethodCallExpr mce = (MethodCallExpr) expr;
                if (mce.getScope().isPresent()) {
                    collectVariableReferencesInExpression(mce.getScope().get());
                }
                for (Expression arg : mce.getArguments()) {
                    collectVariableReferencesInExpression(arg);
                }
            } else if (expr instanceof FieldAccessExpr) {
                FieldAccessExpr fae = (FieldAccessExpr) expr;
                if (fae.getScope() instanceof NameExpr) {
                    usedVariables.add(((NameExpr) fae.getScope()).getNameAsString());
                }
            } else if (expr instanceof ArrayAccessExpr) {
                ArrayAccessExpr aae = (ArrayAccessExpr) expr;
                collectVariableReferencesInExpression(aae.getName());
                collectVariableReferencesInExpression(aae.getIndex());
            } else if (expr instanceof ConditionalExpr) {
                ConditionalExpr ce = (ConditionalExpr) expr;
                collectVariableReferencesInExpression(ce.getCondition());
                collectVariableReferencesInExpression(ce.getThenExpr());
                collectVariableReferencesInExpression(ce.getElseExpr());
            } else if (expr instanceof CastExpr) {
                CastExpr ce = (CastExpr) expr;
                collectVariableReferencesInExpression(ce.getExpression());
            } else if (expr instanceof EnclosedExpr) {
                EnclosedExpr ee = (EnclosedExpr) expr;
                collectVariableReferencesInExpression(ee.getInner());
            }
        }
        
        private boolean isVariableActuallyUsed(String variableName) {
            if (currentMethod == null || !currentMethod.getBody().isPresent()) {
                return false;
            }
            
            String methodBody = currentMethod.getBody().get().toString();
            
            String[] usagePatterns = {
                "\\." + variableName + "\\(",
                variableName + "\\.",
                "\\(" + variableName + "\\)",
                "\\(" + variableName + ",",
                ", " + variableName + "\\)",
                ", " + variableName + ",",
                "= " + variableName + ";",
                "return " + variableName + ";",
                "\\+ " + variableName,
                variableName + " \\+",
                "\\[" + variableName + "\\]",
                variableName + "\\[",
                "if \\(" + variableName,
                "while \\(" + variableName,
                variableName + " ==",
                variableName + " !=",
                "== " + variableName,
                "!= " + variableName
            };
            
            for (String pattern : usagePatterns) {
                if (Pattern.compile(".*" + pattern + ".*", Pattern.DOTALL).matcher(methodBody).matches()) {
                    return true;
                }
            }
            
            return false;
        }
        
        @Override
        public void visit(VariableDeclarator vd, Void arg) {
            checkUnusedVariable(vd);
            checkVariableInitialization(vd);
            super.visit(vd, arg);
        }
        
        @Override
        public void visit(ForStmt fs, Void arg) {
            checkForLoopOptimization(fs);
            checkStringConcatenationInForLoop(fs);
            super.visit(fs, arg);
        }
        
        @Override
        public void visit(WhileStmt ws, Void arg) {
            checkWhileLoopOptimization(ws);
            checkLoopInvariantCalculations(ws);
            checkStringConcatenationInWhileLoop(ws);
            super.visit(ws, arg);
        }
        
        @Override
        public void visit(ForEachStmt fes, Void arg) {
            checkForEachLoopOptimization(fes);
            checkStringConcatenationInForEachLoop(fes);
            super.visit(fes, arg);
        }
        
        @Override
        public void visit(IfStmt is, Void arg) {
            checkIfStatementOptimization(is);
            super.visit(is, arg);
        }
        
        @Override
        public void visit(BinaryExpr be, Void arg) {
            checkBinaryExpressionOptimization(be);
            super.visit(be, arg);
        }
        
        @Override
        public void visit(AssignExpr ae, Void arg) {
            checkStringConcatenationAssignment(ae);
            super.visit(ae, arg);
        }
        
        @Override
        public void visit(ObjectCreationExpr oce, Void arg) {
            checkObjectCreationOptimization(oce);
            super.visit(oce, arg);
        }
        
        private void checkUnusedVariable(VariableDeclarator vd) {
            String variableName = vd.getNameAsString();
            
            if (isCommonUnusedVariable(variableName) || 
                fieldNames.contains(variableName) ||
                isLoopControlVariable(vd) ||
                isParameterVariable(vd)) {
                return;
            }
            
            if (!usedVariables.contains(variableName) && !isVariableActuallyUsed(variableName)) {
                String originalCode = vd.toString();
                String suggestedCode = codeGenerator.generateOptimizationSuggestion(
                    "UNUSED_VARIABLE", vd, currentMethod);
                
                addSuggestion("UNUSED_VARIABLE", 
                            "Local variable '" + variableName + "' is declared but never used",
                            getLineNumber(vd),
                            originalCode,
                            suggestedCode,
                            OptimizationSeverity.LOW);
            }
        }
        
        private void checkVariableInitialization(VariableDeclarator vd) {
            if (!vd.getInitializer().isPresent()) {
                String variableName = vd.getNameAsString();
                String variableType = vd.getTypeAsString();
                
                if (!isPrimitiveType(variableType) && 
                    !isParameterOrField(vd) && 
                    !isLoopVariable(vd) &&
                    !isWhileLoopVariable(vd)) {
                    
                    String originalCode = vd.toString();
                    String suggestedCode = codeGenerator.generateOptimizationSuggestion(
                        "UNINITIALIZED_VARIABLE", vd, currentMethod);
                    
                    addSuggestion("UNINITIALIZED_VARIABLE",
                                "Variable '" + variableName + "' declared without initialization",
                                getLineNumber(vd),
                                originalCode,
                                suggestedCode,
                                OptimizationSeverity.LOW);
                }
            }
        }
        
        private void checkStringConcatenationInForLoop(ForStmt fs) {
            fs.getBody().accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(AssignExpr ae, Void arg) {
                    checkStringConcatenationAssignment(ae);
                    super.visit(ae, arg);
                }
            }, null);
        }
        
        private void checkStringConcatenationInWhileLoop(WhileStmt ws) {
            ws.getBody().accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(AssignExpr ae, Void arg) {
                    checkStringConcatenationAssignment(ae);
                    super.visit(ae, arg);
                }
            }, null);
        }
        
        private void checkStringConcatenationInForEachLoop(ForEachStmt fes) {
            fes.getBody().accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(AssignExpr ae, Void arg) {
                    checkStringConcatenationAssignment(ae);
                    super.visit(ae, arg);
                }
            }, null);
        }
        
        private void checkStringConcatenationAssignment(AssignExpr ae) {
            if (!isInsideActualLoop(ae)) {
                return;
            }
            
            boolean isStringConcatenation = false;
            String targetVar = "";
            
            if (ae.getOperator() == AssignExpr.Operator.PLUS) {
                if (ae.getTarget() instanceof NameExpr) {
                    targetVar = ((NameExpr) ae.getTarget()).getNameAsString();
                    if (isStringVariable(targetVar) || ae.getValue() instanceof StringLiteralExpr) {
                        isStringConcatenation = true;
                    }
                }
            } else if (ae.getOperator() == AssignExpr.Operator.ASSIGN) {
                if (ae.getValue() instanceof BinaryExpr) {
                    BinaryExpr be = (BinaryExpr) ae.getValue();
                    if (be.getOperator() == BinaryExpr.Operator.PLUS) {
                        if (ae.getTarget() instanceof NameExpr && be.getLeft() instanceof NameExpr) {
                            targetVar = ((NameExpr) ae.getTarget()).getNameAsString();
                            String leftVar = ((NameExpr) be.getLeft()).getNameAsString();
                            
                            if (targetVar.equals(leftVar) && 
                                (hasStringOperand(be) || isStringVariable(targetVar))) {
                                isStringConcatenation = true;
                            }
                        }
                    }
                }
            }
            
            if (isStringConcatenation && !isPrintStatement(ae) && !isSimpleLogging(ae)) {
                String originalCode = ae.toString();
                String suggestedCode = codeGenerator.generateOptimizationSuggestion(
                    "STRING_CONCATENATION_IN_LOOP", ae, currentMethod);
                
                addSuggestion("STRING_CONCATENATION_IN_LOOP",
                            "String concatenation inside loop may impact performance",
                            getLineNumber(ae),
                            originalCode,
                            suggestedCode,
                            OptimizationSeverity.HIGH);
            }
        }
        
        private void checkLoopInvariantCalculations(WhileStmt ws) {
            ws.getBody().accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodCallExpr mce, Void arg) {
                    if (isExpensiveCalculation(mce)) {
                        String originalCode = mce.toString();
                        String suggestedCode = codeGenerator.generateOptimizationSuggestion(
                            "LOOP_INVARIANT_CALCULATION", mce, currentMethod);
                        
                        addSuggestion("LOOP_INVARIANT_CALCULATION",
                                    "Expensive calculation inside loop: " + mce.getNameAsString(),
                                    getLineNumber(mce),
                                    originalCode,
                                    suggestedCode,
                                    OptimizationSeverity.MEDIUM);
                    }
                    super.visit(mce, arg);
                }
            }, null);
        }
        
        private void checkObjectCreationOptimization(ObjectCreationExpr oce) {
            String typeName = oce.getTypeAsString();
            
            if ("String".equals(typeName) && oce.getArguments().size() == 0) {
                String originalCode = oce.toString();
                String suggestedCode = codeGenerator.generateOptimizationSuggestion(
                    "UNNECESSARY_OBJECT_CREATION", oce, currentMethod);
                
                addSuggestion("UNNECESSARY_OBJECT_CREATION",
                        "Unnecessary String object creation",
                        getLineNumber(oce),
                        originalCode,
                        suggestedCode,
                        OptimizationSeverity.LOW);
            }
            
            if (("Boolean".equals(typeName) || "Integer".equals(typeName) || "Long".equals(typeName)) 
                && oce.getArguments().size() == 1 && !isInMethodCall(oce)) {
                String originalCode = oce.toString();
                String suggestedCode = codeGenerator.generateOptimizationSuggestion(
                    "WRAPPER_OBJECT_CREATION", oce, currentMethod);
                
                addSuggestion("WRAPPER_OBJECT_CREATION",
                            "Consider using valueOf() method for wrapper objects",
                            getLineNumber(oce),
                            originalCode,
                            suggestedCode,
                            OptimizationSeverity.LOW);
            }
        }
        
        private boolean isInMethodCall(ObjectCreationExpr oce) {
            Node parent = oce.getParentNode().orElse(null);
            return parent instanceof MethodCallExpr;
        }
        private void checkBinaryExpressionOptimization(BinaryExpr be) {
            if (be.getOperator() == BinaryExpr.Operator.DIVIDE) {
                if (be.getRight() instanceof IntegerLiteralExpr) {
                    IntegerLiteralExpr divisor = (IntegerLiteralExpr) be.getRight();
                    int divisorValue = divisor.asInt();
                    if (isPowerOfTwo(divisorValue) && divisorValue > 1) {
                        String originalCode = be.toString();
                        String suggestedCode = codeGenerator.generateOptimizationSuggestion(
                            "DIVISION_OPTIMIZATION", be, currentMethod);
                        
                        addSuggestion("DIVISION_OPTIMIZATION",
                                    "Division by power of 2 can be optimized",
                                    getLineNumber(be),
                                    originalCode,
                                    suggestedCode,
                                    OptimizationSeverity.LOW);
                    }
                }
            }
        }
        
        private void checkMethodOptimization(MethodDeclaration md) {
            if (md.getBody().isPresent()) {
                BlockStmt body = md.getBody().get();
                if (body.getStatements().isEmpty() && !md.isAbstract()) {
                    String originalCode = md.getDeclarationAsString();
                    String suggestedCode = codeGenerator.generateOptimizationSuggestion(
                        "EMPTY_METHOD", md, currentMethod);
                    
                    addSuggestion("EMPTY_METHOD",
                                "Method has empty body",
                                getLineNumber(md),
                                originalCode,
                                suggestedCode,
                                OptimizationSeverity.LOW);
                }
                
                if (md.getParameters().size() > 7) {
                    String originalCode = md.getDeclarationAsString();
                    String suggestedCode = codeGenerator.generateOptimizationSuggestion(
                        "TOO_MANY_PARAMETERS", md, currentMethod);
                    
                    addSuggestion("TOO_MANY_PARAMETERS",
                                "Method has too many parameters (" + md.getParameters().size() + ")",
                                getLineNumber(md),
                                originalCode,
                                suggestedCode,
                                OptimizationSeverity.MEDIUM);
                }
            }
        }
        
        private void checkCyclomaticComplexity(MethodDeclaration md) {
            int complexity = calculateCyclomaticComplexity(md);
            if (complexity > 10) {
                String originalCode = md.getDeclarationAsString();
                String suggestedCode = "Consider refactoring this method to reduce complexity. Current complexity: " + complexity;
                
                addSuggestion("HIGH_CYCLOMATIC_COMPLEXITY",
                            "Method '" + md.getNameAsString() + "' has high cyclomatic complexity: " + complexity,
                            getLineNumber(md),
                            originalCode,
                            suggestedCode,
                            complexity > 20 ? OptimizationSeverity.HIGH : OptimizationSeverity.MEDIUM);
            }
        }
        
        private int calculateCyclomaticComplexity(MethodDeclaration md) {
            class ComplexityVisitor extends VoidVisitorAdapter<Void> {
                int complexity = 1;
                
                @Override
                public void visit(IfStmt n, Void arg) {
                    complexity++;
                    super.visit(n, arg);
                }
                
                @Override
                public void visit(ForStmt n, Void arg) {
                    complexity++;
                    super.visit(n, arg);
                }
                
                @Override
                public void visit(WhileStmt n, Void arg) {
                    complexity++;
                    super.visit(n, arg);
                }
                
                @Override
                public void visit(DoStmt n, Void arg) {
                    complexity++;
                    super.visit(n, arg);
                }
                
                @Override
                public void visit(ForEachStmt n, Void arg) {
                    complexity++;
                    super.visit(n, arg);
                }
                
                @Override
                public void visit(SwitchEntry n, Void arg) {
                    if (!n.getLabels().isEmpty()) {
                        complexity++;
                    }
                    super.visit(n, arg);
                }
                
                @Override
                public void visit(CatchClause n, Void arg) {
                    complexity++;
                    super.visit(n, arg);
                }
                
                @Override
                public void visit(ConditionalExpr n, Void arg) {
                    complexity++;
                    super.visit(n, arg);
                }
                
                @Override
                public void visit(BinaryExpr n, Void arg) {
                    if (n.getOperator() == BinaryExpr.Operator.AND || 
                        n.getOperator() == BinaryExpr.Operator.OR) {
                        complexity++;
                    }
                    super.visit(n, arg);
                }
            }
            
            ComplexityVisitor visitor = new ComplexityVisitor();
            md.accept(visitor, null);
            return visitor.complexity;
        }
        
        private boolean isCommonUnusedVariable(String varName) {
            return varName.startsWith("_") || 
                   varName.equals("args") || 
                   varName.equals("e") ||
                   varName.equals("ex") ||
                   varName.equals("exception") ||
                   varName.equals("ignored");
        }
        
        private boolean isLoopControlVariable(VariableDeclarator vd) {
            Node parent = vd.getParentNode().orElse(null);
            while (parent != null) {
                if (parent instanceof ForStmt || parent instanceof ForEachStmt) {
                    return true;
                }
                if (parent instanceof WhileStmt) {
                    String varName = vd.getNameAsString();
                    String parentStr = parent.toString();
                    return parentStr.contains(varName + " =") && parentStr.contains(varName + "++") ||
                           parentStr.contains(varName + " <") || parentStr.contains(varName + " >");
                }
                parent = parent.getParentNode().orElse(null);
            }
            return false;
        }
        
        private boolean isParameterVariable(VariableDeclarator vd) {
            Node parent = vd.getParentNode().orElse(null);
            while (parent != null) {
                if (parent instanceof com.github.javaparser.ast.body.Parameter) {
                    return true;
                }
                parent = parent.getParentNode().orElse(null);
            }
            return false;
        }
        
        private boolean isPrimitiveType(String type) {
            return type.equals("int") || type.equals("long") || type.equals("double") || 
                   type.equals("float") || type.equals("boolean") || type.equals("char") || 
                   type.equals("byte") || type.equals("short");
        }
        
        private boolean isParameterOrField(VariableDeclarator vd) {
            Node parent = vd.getParentNode().orElse(null);
            while (parent != null) {
                if (parent instanceof FieldDeclaration) {
                    return true;
                }
                String className = parent.getClass().getSimpleName();
                if (className.contains("Parameter")) {
                    return true;
                }
                parent = parent.getParentNode().orElse(null);
            }
            return false;
        }
        
        private boolean isLoopVariable(VariableDeclarator vd) {
            Node parent = vd.getParentNode().orElse(null);
            while (parent != null) {
                if (parent instanceof ForStmt || parent instanceof ForEachStmt) {
                    return true;
                }
                parent = parent.getParentNode().orElse(null);
            }
            return false;
        }
        
        private boolean isWhileLoopVariable(VariableDeclarator vd) {
            String varName = vd.getNameAsString();
            return varName.equals("line") || varName.equals("data") || varName.equals("input");
        }
        
        private boolean hasStringOperand(BinaryExpr be) {
            return be.getLeft() instanceof StringLiteralExpr || 
                   be.getRight() instanceof StringLiteralExpr ||
                   containsStringVariable(be);
        }
        
        private boolean containsStringVariable(BinaryExpr be) {
            return (be.getLeft() instanceof NameExpr && 
                    isStringVariable(((NameExpr) be.getLeft()).getNameAsString())) ||
                   (be.getRight() instanceof NameExpr && 
                    isStringVariable(((NameExpr) be.getRight()).getNameAsString()));
        }
        
        private boolean isStringVariable(String varName) {
            if (currentMethod != null && currentMethod.getBody().isPresent()) {
                String methodBody = currentMethod.getBody().get().toString();
                if (methodBody.contains("String " + varName)) {
                    return true;
                }
            }
            
            return varName.toLowerCase().contains("string") || 
                   varName.toLowerCase().contains("message") ||
                   varName.toLowerCase().contains("text") ||
                   varName.toLowerCase().contains("report") ||
                   varName.toLowerCase().equals("result") ||
                   varName.toLowerCase().contains("output");
        }
        
        private boolean isInsideActualLoop(Node node) {
            Node parent = node.getParentNode().orElse(null);
            while (parent != null) {
                if (parent instanceof ForStmt || parent instanceof WhileStmt || 
                    parent instanceof DoStmt || parent instanceof ForEachStmt) {
                    return true;
                }
                parent = parent.getParentNode().orElse(null);
            }
            return false;
        }
        
        private boolean isPrintStatement(Node node) {
            Node parent = node.getParentNode().orElse(null);
            while (parent != null) {
                if (parent instanceof MethodCallExpr) {
                    MethodCallExpr mce = (MethodCallExpr) parent;
                    if (mce.getNameAsString().contains("print")) {
                        return true;
                    }
                }
                parent = parent.getParentNode().orElse(null);
            }
            return false;
        }
        
        private boolean isSimpleLogging(Node node) {
            Node parent = node.getParentNode().orElse(null);
            while (parent != null) {
                String parentStr = parent.toString().toLowerCase();
                if (parentStr.contains("log") || parentStr.contains("debug") || 
                    parentStr.contains("info") || parentStr.contains("error")) {
                    return true;
                }
                parent = parent.getParentNode().orElse(null);
            }
            return false;
        }
        
        private boolean isExpensiveCalculation(MethodCallExpr mce) {
            String methodName = mce.getNameAsString();
            String scopeName = "";
            if (mce.getScope().isPresent() && mce.getScope().get() instanceof NameExpr) {
                scopeName = ((NameExpr) mce.getScope().get()).getNameAsString();
            }
            
            return ("Math".equals(scopeName) && 
                   ("pow".equals(methodName) || "sqrt".equals(methodName) || 
                    "sin".equals(methodName) || "cos".equals(methodName) ||
                    "log".equals(methodName) || "exp".equals(methodName)));
        }
        
        private void checkForLoopOptimization(ForStmt fs) {
            Optional<Expression> compare = fs.getCompare();
            if (compare.isPresent() && compare.get() instanceof BinaryExpr) {
                BinaryExpr condition = (BinaryExpr) compare.get();
                if (condition.getRight() instanceof MethodCallExpr) {
                    MethodCallExpr methodCall = (MethodCallExpr) condition.getRight();
                    String methodName = methodCall.getNameAsString();
                    
                    if (("size".equals(methodName) || "length".equals(methodName)) &&
                        !isMethodCallOnSimpleVariable(methodCall)) {
                        String originalCode = condition.toString();
                        String suggestedCode = "int size = collection." + methodName + "();\n" +
                                             "for (int i = 0; i < size; i++) { ... }";
                        
                        addSuggestion("INEFFICIENT_LOOP",
                                    "Method call '" + methodName + "()' in loop condition may be inefficient",
                                    getLineNumber(fs),
                                    originalCode,
                                    suggestedCode,
                                    OptimizationSeverity.MEDIUM);
                    }
                }
            }
        }
        
        private void checkWhileLoopOptimization(WhileStmt ws) {
            Expression condition = ws.getCondition();
            if (condition instanceof BooleanLiteralExpr) {
                BooleanLiteralExpr bool = (BooleanLiteralExpr) condition;
                if (bool.getValue() && !hasBreakStatement(ws.getBody())) {
                    String originalCode = ws.toString();
                    String suggestedCode = "while (condition) {\n" +
                                         "    if (exitCondition) break;\n" +
                                         "}";
                    
                    addSuggestion("INFINITE_LOOP",
                                "Potential infinite loop detected - no break statement found",
                                getLineNumber(ws),
                                originalCode,
                                suggestedCode,
                                OptimizationSeverity.HIGH);
                }
            }
        }
        
        private void checkForEachLoopOptimization(ForEachStmt fes) {
        }
        
        private void checkIfStatementOptimization(IfStmt is) {
            Expression condition = is.getCondition();
            if (condition instanceof BooleanLiteralExpr) {
                String originalCode = condition.toString();
                String suggestedCode = "if (true) { ... } can be simplified to just: { ... }\n" +
                                     "if (false) { ... } can be removed entirely";
                
                addSuggestion("REDUNDANT_CONDITION",
                            "If statement with constant boolean condition",
                            getLineNumber(is),
                            originalCode,
                            suggestedCode,
                            OptimizationSeverity.LOW);
            }
        }
        
        private boolean isMethodCallOnSimpleVariable(MethodCallExpr methodCall) {
            return methodCall.getScope().isPresent() && 
                   methodCall.getScope().get() instanceof NameExpr;
        }
        
        private boolean hasBreakStatement(Statement stmt) {
            if (stmt instanceof BreakStmt) {
                return true;
            }
            if (stmt instanceof BlockStmt) {
                BlockStmt block = (BlockStmt) stmt;
                return block.getStatements().stream().anyMatch(this::hasBreakStatement);
            }
            return false;
        }
        
        private boolean isPowerOfTwo(int n) {
            return n > 0 && (n & (n - 1)) == 0;
        }
        
        private int getLineNumber(Node node) {
            return node.getBegin().map(pos -> pos.line).orElse(0);
        }
        
        private void addSuggestion(String type, String description, int lineNumber, 
                                 String originalCode, String suggestedCode, 
                                 OptimizationSeverity severity) {
            suggestions.add(new OptimizationSuggestion(type, description, lineNumber, 
                                                      originalCode, suggestedCode, severity));
        }
    }
}