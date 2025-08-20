package application.models;

public class OptimizationSuggestion {
    
    private final String type;
    private final String description;
    private final int lineNumber;
    private final String originalCode;
    private final String suggestedCode;
    private final OptimizationSeverity severity;
    
    public OptimizationSuggestion(String type, String description, int lineNumber, 
                                String originalCode, String suggestedCode, 
                                OptimizationSeverity severity) {
        this.type = type;
        this.description = description;
        this.lineNumber = lineNumber;
        this.originalCode = originalCode;
        this.suggestedCode = suggestedCode;
        this.severity = severity;
    }
    
    public String getType() { return type; }
    public String getDescription() { return description; }
    public int getLineNumber() { return lineNumber; }
    public String getOriginalCode() { return originalCode; }
    public String getSuggestedCode() { return suggestedCode; }
    public OptimizationSeverity getSeverity() { return severity; }
}