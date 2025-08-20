package application.services;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import application.models.SecurityIssue;
import application.models.SecuritySeverity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SecurityService {
    
    private static final List<String> DANGEROUS_RUNTIME_METHODS = Arrays.asList(
        "exec", "getRuntime", "ProcessBuilder"
    );
    
    private static final List<String> SQL_METHODS = Arrays.asList(
        "executeQuery", "executeUpdate", "execute"
    );
    
    private static final List<String> FILE_OPERATIONS = Arrays.asList(
        "FileInputStream", "FileOutputStream", "FileReader", "FileWriter",
        "RandomAccessFile", "File", "Files"
    );
    
    private final CSGenerator codeGenerator = new CSGenerator();
    
    public List<SecurityIssue> analyzeSecurityIssues(CompilationUnit compilationUnit) {
        List<SecurityIssue> issues = new ArrayList<>();
        
        SecurityAnalyzer analyzer = new SecurityAnalyzer(issues);
        compilationUnit.accept(analyzer, null);
        
        return issues;
    }
    
    private class SecurityAnalyzer extends VoidVisitorAdapter<Void> {
        
        private final List<SecurityIssue> issues;
        private final Set<String> nullCheckedVariables = new HashSet<>();
        private final Set<String> initializedVariables = new HashSet<>();
        private final Set<String> parameterNames = new HashSet<>();
        private final Set<String> fieldNames = new HashSet<>();
        private final Set<String> exceptionVariables = new HashSet<>();
        private final Set<String> enhancedForLoopVariables = new HashSet<>();
        private final Set<Integer> securityIssueLines = new HashSet<>();
        private MethodDeclaration currentMethod;
        
        public SecurityAnalyzer(List<SecurityIssue> issues) {
            this.issues = issues;
        }
        
        @Override
        public void visit(CompilationUnit cu, Void arg) {
            collectFieldNames(cu);
            super.visit(cu, arg);
        }
        
        private void collectFieldNames(CompilationUnit cu) {
            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(com.github.javaparser.ast.body.FieldDeclaration fd, Void arg) {
                    fd.getVariables().forEach(var -> fieldNames.add(var.getNameAsString()));
                    super.visit(fd, arg);
                }
            }, null);
        }
        
        @Override
        public void visit(MethodDeclaration md, Void arg) {
            this.currentMethod = md;
            nullCheckedVariables.clear();
            initializedVariables.clear();
            parameterNames.clear();
            exceptionVariables.clear();
            enhancedForLoopVariables.clear();
            
            md.getParameters().forEach(param -> parameterNames.add(param.getNameAsString()));
            
            collectNullChecks(md);
            collectInitializedVariables(md);
            collectExceptionVariables(md);
            collectEnhancedForLoopVariables(md);
            
            super.visit(md, arg);
        }
        
        @Override
        public void visit(com.github.javaparser.ast.body.FieldDeclaration fd, Void arg) {
            checkHardcodedCredentialsInFields(fd);
            super.visit(fd, arg);
        }
        
        @Override
        public void visit(StringLiteralExpr sle, Void arg) {
            checkHardcodedCredentialsInStrings(sle);
            super.visit(sle, arg);
        }
        
        @Override
        public void visit(ObjectCreationExpr oce, Void arg) {
            checkUnsafeObjectCreation(oce);
            checkFileOperationSecurity(oce);
            super.visit(oce, arg);
        }
        
        private void collectEnhancedForLoopVariables(Node node) {
            node.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(ForEachStmt fes, Void arg) {
                    enhancedForLoopVariables.add(fes.getVariable().getVariables().get(0).getNameAsString());
                    super.visit(fes, arg);
                }
            }, null);
        }
        
        private void collectNullChecks(Node node) {
            node.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(BinaryExpr be, Void arg) {
                    if (be.getOperator() == BinaryExpr.Operator.NOT_EQUALS) {
                        if (be.getRight() instanceof NullLiteralExpr && be.getLeft() instanceof NameExpr) {
                            nullCheckedVariables.add(((NameExpr) be.getLeft()).getNameAsString());
                        }
                        if (be.getLeft() instanceof NullLiteralExpr && be.getRight() instanceof NameExpr) {
                            nullCheckedVariables.add(((NameExpr) be.getRight()).getNameAsString());
                        }
                    }
                    super.visit(be, arg);
                }
                
                @Override
                public void visit(MethodCallExpr mce, Void arg) {
                    String methodName = mce.getNameAsString();
                    String className = getClassName(mce);
                    
                    if ("Objects".equals(className) && 
                        ("nonNull".equals(methodName) || "requireNonNull".equals(methodName))) {
                        if (!mce.getArguments().isEmpty() && mce.getArguments().get(0) instanceof NameExpr) {
                            nullCheckedVariables.add(((NameExpr) mce.getArguments().get(0)).getNameAsString());
                        }
                    }
                    super.visit(mce, arg);
                }
            }, null);
        }
        
        private void collectInitializedVariables(Node node) {
            node.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(VariableDeclarationExpr vde, Void arg) {
                    vde.getVariables().forEach(var -> {
                        if (var.getInitializer().isPresent()) {
                            initializedVariables.add(var.getNameAsString());
                        }
                    });
                    super.visit(vde, arg);
                }
                
                @Override
                public void visit(AssignExpr ae, Void arg) {
                    if (ae.getTarget() instanceof NameExpr) {
                        initializedVariables.add(((NameExpr) ae.getTarget()).getNameAsString());
                    }
                    super.visit(ae, arg);
                }
            }, null);
        }
        
        private void collectExceptionVariables(Node node) {
            node.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(CatchClause cc, Void arg) {
                    exceptionVariables.add(cc.getParameter().getNameAsString());
                    super.visit(cc, arg);
                }
            }, null);
        }
        
        @Override
        public void visit(MethodCallExpr mce, Void arg) {
            checkDangerousMethodCalls(mce);
            checkSQLInjectionVulnerability(mce);
            checkNullPointerDeReference(mce);
            super.visit(mce, arg);
        }
        
        @Override
        public void visit(VariableDeclarationExpr vde, Void arg) {
            checkSensitiveDataExposure(vde);
            super.visit(vde, arg);
        }
        
        @Override
        public void visit(ArrayAccessExpr aae, Void arg) {
            checkArrayBoundsVulnerability(aae);
            super.visit(aae, arg);
        }
        
        @Override
        public void visit(CastExpr ce, Void arg) {
            checkUnsafeCasting(ce);
            super.visit(ce, arg);
        }
        
        @Override
        public void visit(TryStmt ts, Void arg) {
            checkImproperExceptionHandling(ts);
            super.visit(ts, arg);
        }
        
        private void checkHardcodedCredentialsInFields(com.github.javaparser.ast.body.FieldDeclaration fd) {
            fd.getVariables().forEach(variable -> {
                String variableName = variable.getNameAsString().toLowerCase();
                
                if (isCredentialFieldName(variableName) && variable.getInitializer().isPresent()) {
                    Expression initializer = variable.getInitializer().get();
                    
                    if (initializer instanceof StringLiteralExpr && !isFromSecureSource(variable)) {
                        StringLiteralExpr stringLiteral = (StringLiteralExpr) initializer;
                        String value = stringLiteral.getValue();
                        
                        if (!value.isEmpty() && !isPlaceholderValue(value)) {
                            String originalCode = variable.toString();
                            String suggestedCode = codeGenerator.generateSecuritySuggestion(
                                "HARDCODED_CREDENTIALS", fd, currentMethod);
                            
                            addSecurityIssue("HARDCODED_CREDENTIALS",
                                           "Hardcoded credential found in field: " + variable.getNameAsString(),
                                           getLineNumber(fd),
                                           originalCode,
                                           suggestedCode,
                                           SecuritySeverity.CRITICAL);
                        }
                    }
                }
            });
        }
        
        private void checkHardcodedCredentialsInStrings(StringLiteralExpr sle) {
            String value = sle.getValue();
            
            if (looksLikeCredential(value) && !isInTestContext(sle) && !isInConfigContext(sle)) {
                String originalCode = sle.toString();
                String suggestedCode = codeGenerator.generateSecuritySuggestion(
                    "HARDCODED_CREDENTIALS", sle, currentMethod);
                
                addSecurityIssue("HARDCODED_CREDENTIALS",
                               "Potential hardcoded credential in string literal",
                               getLineNumber(sle),
                               originalCode,
                               suggestedCode,
                               SecuritySeverity.HIGH);
            }
        }
        
        private boolean isCredentialFieldName(String fieldName) {
            return fieldName.contains("password") || 
                   fieldName.contains("secret") ||
                   fieldName.contains("key") ||
                   fieldName.contains("token") ||
                   fieldName.contains("user") ||
                   fieldName.contains("username") ||
                   fieldName.contains("credential") ||
                   fieldName.contains("auth") ||
                   fieldName.contains("api_key") ||
                   fieldName.contains("access_key");
        }
        
        private boolean isPlaceholderValue(String value) {
            String lowerValue = value.toLowerCase();
            return lowerValue.equals("password") ||
                   lowerValue.equals("secret") ||
                   lowerValue.equals("user") ||
                   lowerValue.equals("test") ||
                   lowerValue.equals("example") ||
                   lowerValue.equals("demo") ||
                   lowerValue.equals("default") ||
                   lowerValue.equals("changeme") ||
                   lowerValue.equals("") ||
                   lowerValue.startsWith("todo") ||
                   lowerValue.startsWith("placeholder") ||
                   lowerValue.startsWith("your_") ||
                   lowerValue.startsWith("enter_");
        }
        
        private boolean looksLikeCredential(String value) {
            if (value.length() < 3) return false;
            
            String lowerValue = value.toLowerCase();
            
            if (looksLikePassword(value) || 
                looksLikeApiKey(value) || 
                looksLikeToken(value)) {
                return true;
            }
            
            return lowerValue.equals("admin") ||
                   lowerValue.equals("administrator") ||
                   lowerValue.equals("root") ||
                   lowerValue.equals("sa") ||
                   (lowerValue.contains("admin") && value.length() < 20);
        }
        
        private boolean looksLikePassword(String value) {
            if (value.length() < 6) return false;
            
            boolean hasUpper = value.chars().anyMatch(Character::isUpperCase);
            boolean hasLower = value.chars().anyMatch(Character::isLowerCase);
            boolean hasDigit = value.chars().anyMatch(Character::isDigit);
            boolean hasSpecial = value.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
            
            int complexityScore = (hasUpper ? 1 : 0) + (hasLower ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
            return complexityScore >= 2;
        }
        
        private boolean looksLikeApiKey(String value) {
            if (value.length() < 16 || value.length() > 128) return false;
            
            long alphanumericCount = value.chars().filter(ch -> Character.isLetterOrDigit(ch) || ch == '-' || ch == '_').count();
            double alphanumericRatio = (double) alphanumericCount / value.length();
            
            return alphanumericRatio > 0.8;
        }
        
        private boolean looksLikeToken(String value) {
            return value.contains(".") && value.length() > 20 ||
                   (value.length() > 32 && value.chars().allMatch(ch -> Character.isLetterOrDigit(ch)));
        }
        
        private boolean isInTestContext(StringLiteralExpr sle) {
            Node parent = sle;
            while (parent != null) {
                if (parent instanceof MethodDeclaration) {
                    MethodDeclaration method = (MethodDeclaration) parent;
                    String methodName = method.getNameAsString().toLowerCase();
                    if (methodName.contains("test") || methodName.equals("main")) {
                        return true;
                    }
                }
                parent = parent.getParentNode().orElse(null);
            }
            return false;
        }
        
        private boolean isInConfigContext(StringLiteralExpr sle) {
            Node parent = sle.getParentNode().orElse(null);
            while (parent != null) {
                String parentStr = parent.toString();
                if (parentStr.contains("System.getProperty") ||
                    parentStr.contains("System.getenv") ||
                    parentStr.contains("config") ||
                    parentStr.contains("properties")) {
                    return true;
                }
                parent = parent.getParentNode().orElse(null);
            }
            return false;
        }
        
        private void checkDangerousMethodCalls(MethodCallExpr mce) {
            String methodName = mce.getNameAsString();
            String fullCall = mce.toString();
            
            if ("exec".equals(methodName) && isRuntimeExec(mce)) {
                if (!isInTestMethod(mce)) {
                    String originalCode = mce.toString();
                    String suggestedCode = codeGenerator.generateSecuritySuggestion(
                        "COMMAND_INJECTION", mce, currentMethod);
                    
                    int lineNumber = getLineNumber(mce);
                    securityIssueLines.add(lineNumber);
                    
                    addSecurityIssue("COMMAND_INJECTION",
                                   "Potentially dangerous Runtime.exec() call",
                                   lineNumber,
                                   originalCode,
                                   suggestedCode,
                                   SecuritySeverity.HIGH);
                }
            }
            
            if ("getRuntime".equals(methodName) && fullCall.contains("Runtime.getRuntime()")) {
                if (!isInTestMethod(mce)) {
                    String originalCode = mce.toString();
                    String suggestedCode = codeGenerator.generateSecuritySuggestion(
                        "COMMAND_INJECTION", mce, currentMethod);
                    
                    addSecurityIssue("DANGEROUS_METHOD_CALL",
                                   "Use of Runtime.getRuntime() detected",
                                   getLineNumber(mce),
                                   originalCode,
                                   suggestedCode,
                                   SecuritySeverity.MEDIUM);
                }
            }
        }
        
        private void checkSQLInjectionVulnerability(MethodCallExpr mce) {
            String methodName = mce.getNameAsString();
            
            for (String sqlMethod : SQL_METHODS) {
                if (methodName.equals(sqlMethod)) {
                    int lineNumber = getLineNumber(mce);
                    securityIssueLines.add(lineNumber);
                    
                    if (hasStringConcatenationInArguments(mce) && !isPreparedStatement(mce)) {
                        String originalCode = mce.toString();
                        String suggestedCode = codeGenerator.generateSecuritySuggestion(
                            "SQL_INJECTION", mce, currentMethod);
                        
                        addSecurityIssue("SQL_INJECTION",
                                       "SQL injection vulnerability - string concatenation in query",
                                       lineNumber,
                                       originalCode,
                                       suggestedCode,
                                       SecuritySeverity.CRITICAL);
                    } else if (hasVariableInArguments(mce) && !isPreparedStatement(mce)) {
                        checkForDynamicSQLConstruction(mce);
                    }
                    break;
                }
            }
        }
        
        private void checkForDynamicSQLConstruction(MethodCallExpr mce) {
            for (Expression arg : mce.getArguments()) {
                if (arg instanceof NameExpr) {
                    NameExpr nameExpr = (NameExpr) arg;
                    String varName = nameExpr.getNameAsString();
                    
                    if (varName.toLowerCase().contains("query") || 
                        varName.toLowerCase().contains("sql") ||
                        varName.toLowerCase().contains("statement")) {
                        
                        if (isDynamicSQLVariable(varName)) {
                            String originalCode = mce.toString();
                            String suggestedCode = codeGenerator.generateSecuritySuggestion(
                                "SQL_INJECTION", mce, currentMethod);
                            
                            addSecurityIssue("DYNAMIC_SQL_CONSTRUCTION",
                                           "Dynamic SQL query construction detected",
                                           getLineNumber(mce),
                                           originalCode,
                                           suggestedCode,
                                           SecuritySeverity.CRITICAL);
                        }
                    }
                }
            }
        }
        
        private boolean isDynamicSQLVariable(String varName) {
            if (currentMethod == null) return false;
            
            return currentMethod.getBody().isPresent() && 
                   currentMethod.getBody().get().toString().contains(varName + " = ") &&
                   currentMethod.getBody().get().toString().contains(" + ");
        }
        
        private void checkFileOperationSecurity(ObjectCreationExpr oce) {
            String typeName = oce.getTypeAsString();
            
            if ("File".equals(typeName) || "FileInputStream".equals(typeName) || 
                "FileOutputStream".equals(typeName) || "FileReader".equals(typeName) || 
                "FileWriter".equals(typeName)) {
                
                for (Expression arg : oce.getArguments()) {
                    if (containsStringConcatenation(arg) || isUserInput(arg)) {
                        String originalCode = oce.toString();
                        String suggestedCode = codeGenerator.generateSecuritySuggestion(
                            "PATH_TRAVERSAL", oce, currentMethod);
                        
                        int lineNumber = getLineNumber(oce);
                        securityIssueLines.add(lineNumber);
                        
                        addSecurityIssue("PATH_TRAVERSAL",
                                       "Potential path traversal vulnerability - user input in file path",
                                       lineNumber,
                                       originalCode,
                                       suggestedCode,
                                       SecuritySeverity.HIGH);
                        break;
                    }
                }
            }
        }
        
        private boolean containsStringConcatenation(Expression expr) {
            if (expr instanceof BinaryExpr) {
                BinaryExpr be = (BinaryExpr) expr;
                return be.getOperator() == BinaryExpr.Operator.PLUS && 
                       (hasStringOperand(be) || containsStringConcatenation(be.getLeft()) || 
                        containsStringConcatenation(be.getRight()));
            }
            return false;
        }
        
        private boolean hasStringOperand(BinaryExpr be) {
            return be.getLeft() instanceof StringLiteralExpr || 
                   be.getRight() instanceof StringLiteralExpr ||
                   be.getLeft() instanceof NameExpr || 
                   be.getRight() instanceof NameExpr;
        }
        
        private void checkNullPointerDeReference(MethodCallExpr mce) {
            if (mce.getScope().isPresent()) {
                Expression scope = mce.getScope().get();
                if (scope instanceof NameExpr) {
                    NameExpr nameExpr = (NameExpr) scope;
                    String varName = nameExpr.getNameAsString();
                    
                    if (shouldCheckForNull(varName, mce)) {
                        String originalCode = mce.toString();
                        String suggestedCode = codeGenerator.generateSecuritySuggestion(
                            "NULL_POINTER_DEREFERENCE", mce, currentMethod);
                        
                        addSecurityIssue("NULL_POINTER_DEREFERENCE",
                                       "Potential null pointer dereference on variable: " + varName,
                                       getLineNumber(mce),
                                       originalCode,
                                       suggestedCode,
                                       SecuritySeverity.MEDIUM);
                    }
                }
            }
        }
        
        private void checkUnsafeObjectCreation(ObjectCreationExpr oce) {
            String typeName = oce.getTypeAsString();
            
            if ("Random".equals(typeName)) {
                if (isSecurityCriticalContext(oce)) {
                    String originalCode = oce.toString();
                    String suggestedCode = codeGenerator.generateSecuritySuggestion(
                        "WEAK_RANDOM", oce, currentMethod);
                    
                    addSecurityIssue("WEAK_RANDOM",
                                   "Using weak random number generator in security context",
                                   getLineNumber(oce),
                                   originalCode,
                                   suggestedCode,
                                   SecuritySeverity.MEDIUM);
                }
            }
        }
        
        private void checkSensitiveDataExposure(VariableDeclarationExpr vde) {
            String variableType = vde.getElementType().asString();
            
            vde.getVariables().forEach(variable -> {
                String variableName = variable.getNameAsString().toLowerCase();
                if ((variableName.contains("password") || variableName.contains("secret") || 
                     variableName.contains("key") || variableName.contains("token")) &&
                    "String".equals(variableType) &&
                    !isConstantDeclaration(variable) &&
                    !isFromSecureSource(variable)) {
                    
                    String originalCode = variable.toString();
                    String suggestedCode = codeGenerator.generateSecuritySuggestion(
                        "SENSITIVE_DATA_EXPOSURE", vde, currentMethod);
                    
                    addSecurityIssue("SENSITIVE_DATA_EXPOSURE",
                                   "Sensitive data stored in String (immutable and may appear in memory dumps)",
                                   getLineNumber(vde),
                                   originalCode,
                                   suggestedCode,
                                   SecuritySeverity.MEDIUM);
                }
            });
        }
        
        private boolean isFromSecureSource(com.github.javaparser.ast.body.VariableDeclarator variable) {
            if (!variable.getInitializer().isPresent()) return false;
            
            String init = variable.getInitializer().get().toString();
            return init.contains("System.getProperty") ||
                   init.contains("System.getenv") ||
                   init.contains("SecureRandom") ||
                   init.contains("KeyGenerator") ||
                   init.contains("getPassword()");
        }
        
        private void checkArrayBoundsVulnerability(ArrayAccessExpr aae) {
            Expression index = aae.getIndex();
            
            boolean isRisky = false;
            
            if (index instanceof NameExpr) {
                String indexVar = ((NameExpr) index).getNameAsString();
                isRisky = parameterNames.contains(indexVar) || 
                          indexVar.toLowerCase().contains("index") ||
                          indexVar.toLowerCase().contains("pos") ||
                          indexVar.toLowerCase().contains("offset");
            }
            
            if (isRisky && !hasArrayBoundsCheck(aae)) {
                String originalCode = aae.toString();
                String suggestedCode = codeGenerator.generateSecuritySuggestion(
                    "ARRAY_BOUNDS_CHECK", aae, currentMethod);
                
                addSecurityIssue("ARRAY_BOUNDS_CHECK",
                               "Array access without bounds checking - potential ArrayIndexOutOfBoundsException",
                               getLineNumber(aae),
                               originalCode,
                               suggestedCode,
                               SecuritySeverity.MEDIUM);
            }
        }
        private void checkUnsafeCasting(CastExpr ce) {
            if (!hasInstanceOfCheck(ce) && !isSimplePrimitiveCast(ce) && !isTrivialCast(ce)) {
                String originalCode = ce.toString();
                String suggestedCode = codeGenerator.generateSecuritySuggestion(
                    "UNSAFE_CASTING", ce, currentMethod);
                
                addSecurityIssue("UNSAFE_CASTING",
                               "Unsafe type casting without instanceof check",
                               getLineNumber(ce),
                               originalCode,
                               suggestedCode,
                               SecuritySeverity.LOW);
            }
        }
        
        private void checkImproperExceptionHandling(TryStmt ts) {
            List<CatchClause> catchClauses = ts.getCatchClauses();
            
            for (CatchClause catchClause : catchClauses) {
                if (catchClause.getBody().getStatements().isEmpty()) {
                    String originalCode = catchClause.toString();
                    String suggestedCode = codeGenerator.generateSecuritySuggestion(
                        "EMPTY_CATCH_BLOCK", ts, currentMethod);
                    
                    addSecurityIssue("EMPTY_CATCH_BLOCK",
                                   "Empty catch block may hide security issues",
                                   getLineNumber(catchClause),
                                   originalCode,
                                   suggestedCode,
                                   SecuritySeverity.LOW);
                } else if (catchClause.getBody().getStatements().size() == 1) {
                    Statement stmt = catchClause.getBody().getStatements().get(0);
                    if (stmt instanceof ExpressionStmt) {
                        ExpressionStmt exprStmt = (ExpressionStmt) stmt;
                        if (exprStmt.getExpression() instanceof MethodCallExpr) {
                            MethodCallExpr mce = (MethodCallExpr) exprStmt.getExpression();
                            if ("printStackTrace".equals(mce.getNameAsString())) {
                                String originalCode = catchClause.toString();
                                String suggestedCode = codeGenerator.generateSecuritySuggestion(
                                    "POOR_EXCEPTION_HANDLING", ts, currentMethod);
                                
                                addSecurityIssue("POOR_EXCEPTION_HANDLING",
                                               "Exception handling only prints stack trace - consider proper logging",
                                               getLineNumber(catchClause),
                                               originalCode,
                                               suggestedCode,
                                               SecuritySeverity.LOW);
                            }
                        }
                    }
                }
            }
        }
        
        private boolean shouldCheckForNull(String varName, MethodCallExpr mce) {
            int lineNumber = getLineNumber(mce);
            if (securityIssueLines.contains(lineNumber)) {
                return false;
            }
            
            if (isInSecurityCriticalMethod()) {
                return false;
            }
            
            if (parameterNames.contains(varName) && !nullCheckedVariables.contains(varName)) {
                return isDirectlyDereferenced(varName);
            }
            
            if (isPotentiallyNullMethodReturn(varName)) {
                return !nullCheckedVariables.contains(varName);
            }
            
            return !nullCheckedVariables.contains(varName) && 
                   !initializedVariables.contains(varName) &&
                   !fieldNames.contains(varName) &&
                   !exceptionVariables.contains(varName) &&
                   !enhancedForLoopVariables.contains(varName) &&
                   !isKnownNonNullType(varName) &&
                   !isThisOrSuper(varName) &&
                   !isConstantOrStaticCall(varName);
        }
        
        private boolean isInSecurityCriticalMethod() {
            if (currentMethod != null) {
                String methodName = currentMethod.getNameAsString().toLowerCase();
                return methodName.contains("sql") || methodName.contains("query") ||
                       methodName.contains("execute") || methodName.contains("command");
            }
            return false;
        }
        
        private boolean isDirectlyDereferenced(String varName) {
            if (currentMethod != null && currentMethod.getBody().isPresent()) {
                String methodBody = currentMethod.getBody().get().toString();
                return methodBody.matches(".*\\b" + varName + "\\.\\w+\\(.*");
            }
            return false;
        }
        
        private boolean isPotentiallyNullMethodReturn(String varName) {
            if (currentMethod != null && currentMethod.getBody().isPresent()) {
                String methodBody = currentMethod.getBody().get().toString();
                return methodBody.contains(varName + " = ") && 
                       (methodBody.contains(".get") || methodBody.contains("find") || 
                        methodBody.contains("search") || methodBody.contains("lookup"));
            }
            return false;
        }
        
        private boolean isRuntimeExec(MethodCallExpr mce) {
            if (!mce.getScope().isPresent()) return false;
            
            Expression scope = mce.getScope().get();
            return scope.toString().contains("Runtime.getRuntime()") ||
                   scope.toString().contains("runtime");
        }
        
        private boolean isInTestMethod(Node node) {
            Node parent = node;
            while (parent != null) {
                if (parent instanceof MethodDeclaration) {
                    MethodDeclaration method = (MethodDeclaration) parent;
                    String methodName = method.getNameAsString().toLowerCase();
                    return methodName.contains("test") || methodName.startsWith("test") ||
                           methodName.equals("main");
                }
                parent = parent.getParentNode().orElse(null);
            }
            return false;
        }
        
        private boolean isPreparedStatement(MethodCallExpr mce) {
            if (!mce.getScope().isPresent()) return false;
            
            return mce.getScope().get().toString().toLowerCase().contains("preparedstatement") ||
                   mce.getScope().get().toString().toLowerCase().contains("statement");
        }
        
        private boolean hasStringConcatenationInArguments(MethodCallExpr mce) {
            return mce.getArguments().stream()
                     .anyMatch(arg -> arg instanceof BinaryExpr && 
                              ((BinaryExpr) arg).getOperator() == BinaryExpr.Operator.PLUS &&
                              containsVariableReference((BinaryExpr) arg));
        }
        
        private boolean hasVariableInArguments(MethodCallExpr mce) {
            return mce.getArguments().stream()
                     .anyMatch(arg -> arg instanceof NameExpr);
        }
        
        private boolean containsVariableReference(BinaryExpr be) {
            return (be.getLeft() instanceof NameExpr || be.getRight() instanceof NameExpr) ||
                   (be.getLeft() instanceof BinaryExpr && containsVariableReference((BinaryExpr) be.getLeft())) ||
                   (be.getRight() instanceof BinaryExpr && containsVariableReference((BinaryExpr) be.getRight()));
        }
        
        private boolean hasPathValidation(MethodCallExpr mce) {
            Node parent = mce.getParentNode().orElse(null);
            while (parent != null && !(parent instanceof MethodDeclaration)) {
                if (parent.toString().contains("normalize") || 
                    parent.toString().contains("validate") ||
                    parent.toString().contains("canonical")) {
                    return true;
                }
                parent = parent.getParentNode().orElse(null);
            }
            return false;
        }
        
        private boolean isKnownNonNullType(String varName) {
            return Character.isUpperCase(varName.charAt(0)) ||
                   varName.equals("System") ||
                   varName.equals("Math") ||
                   varName.equals("String") ||
                   varName.equals("Objects");
        }
        
        private boolean isThisOrSuper(String varName) {
            return varName.equals("this") || varName.equals("super");
        }
        
        private boolean isConstantOrStaticCall(String varName) {
            return varName.toUpperCase().equals(varName) ||
                   (Character.isUpperCase(varName.charAt(0)) && varName.length() > 1);
        }
        
        private boolean isMethodResult(String varName) {
            return varName.startsWith("get") || varName.startsWith("create") || 
                   varName.startsWith("build") || varName.startsWith("new");
        }
        
        private boolean isSecurityCriticalContext(Node node) {
            Node parent = node;
            while (parent != null) {
                String nodeStr = parent.toString().toLowerCase();
                if (nodeStr.contains("password") || nodeStr.contains("key") || 
                    nodeStr.contains("token") || nodeStr.contains("crypto") ||
                    nodeStr.contains("security") || nodeStr.contains("auth")) {
                    return true;
                }
                parent = parent.getParentNode().orElse(null);
            }
            return false;
        }
        
        private boolean isConstantDeclaration(com.github.javaparser.ast.body.VariableDeclarator variable) {
            Node parent = variable.getParentNode().orElse(null);
            while (parent != null) {
                if (parent instanceof com.github.javaparser.ast.body.FieldDeclaration) {
                    com.github.javaparser.ast.body.FieldDeclaration fd = (com.github.javaparser.ast.body.FieldDeclaration) parent;
                    return fd.isStatic() && fd.isFinal();
                }
                parent = parent.getParentNode().orElse(null);
            }
            return false;
        }
        
        private boolean hasArrayBoundsCheck(ArrayAccessExpr aae) {
            Node current = aae.getParentNode().orElse(null);
            String arrayName = aae.getName().toString();
            String indexName = aae.getIndex().toString();
            
            while (current != null && !(current instanceof BlockStmt) && !(current instanceof MethodDeclaration)) {
                current = current.getParentNode().orElse(null);
            }
            
            if (current != null) {
                String context = current.toString();
                return context.contains("if (" + indexName + " >= 0") ||
                       context.contains("if (" + indexName + " < " + arrayName + ".length") ||
                       context.contains(indexName + " >= 0 && " + indexName + " < ") ||
                       context.contains("checkBounds") ||
                       context.contains("isValidIndex");
            }
            
            return false;
        }
        
        private boolean isUserControlledInput(Expression expr) {
            if (expr instanceof NameExpr) {
                String varName = ((NameExpr) expr).getNameAsString().toLowerCase();
                return (varName.contains("input") || varName.contains("param") || 
                       varName.contains("request") || varName.contains("user")) &&
                       parameterNames.contains(((NameExpr) expr).getNameAsString());
            }
            return false;
        }
        
        private boolean isSimplePrimitiveCast(CastExpr ce) {
            String targetType = ce.getTypeAsString();
            return targetType.equals("int") || targetType.equals("double") || 
                   targetType.equals("float") || targetType.equals("long") ||
                   targetType.equals("short") || targetType.equals("byte");
        }
        
        private boolean isTrivialCast(CastExpr ce) {
            String targetType = ce.getTypeAsString();
            String expr = ce.getExpression().toString();
            
            return targetType.equals("String") && 
                   (expr.contains("toString()") || expr.contains("String.valueOf"));
        }
        
        private boolean hasInstanceOfCheck(CastExpr ce) {
            Node parent = ce.getParentNode().orElse(null);
            while (parent != null && !(parent instanceof MethodDeclaration)) {
                if (parent.toString().contains("instanceof")) {
                    return true;
                }
                parent = parent.getParentNode().orElse(null);
            }
            return false;
        }
        
        private boolean hasUserInputInArguments(MethodCallExpr mce) {
            return mce.getArguments().stream()
                     .anyMatch(arg -> isUserInput(arg));
        }
        
        private boolean isUserInput(Expression expr) {
            if (expr instanceof NameExpr) {
                String varName = ((NameExpr) expr).getNameAsString().toLowerCase();
                return (varName.contains("input") || varName.contains("param") || 
                       varName.contains("request") || varName.contains("user")) &&
                       parameterNames.contains(((NameExpr) expr).getNameAsString());
            }
            return false;
        }
        
        private String getClassName(MethodCallExpr mce) {
            if (mce.getScope().isPresent() && mce.getScope().get() instanceof NameExpr) {
                return ((NameExpr) mce.getScope().get()).getNameAsString();
            }
            return "";
        }
        
        private int getLineNumber(Node node) {
            return node.getBegin().map(pos -> pos.line).orElse(0);
        }
        
        private void addSecurityIssue(String type, String description, int lineNumber,
                                    String vulnerableCode, String recommendation,
                                    SecuritySeverity severity) {
            issues.add(new SecurityIssue(type, description, lineNumber, 
                                        vulnerableCode, recommendation, severity));
        }
    }
}