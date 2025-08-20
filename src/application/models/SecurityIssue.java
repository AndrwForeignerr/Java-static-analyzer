package application.models;

public class SecurityIssue {
    
    private final String type;
    private final String description;
    private final int lineNumber;
    private final String vulnerableCode;
    private final String recommendation;
    private final SecuritySeverity severity;
    
    public SecurityIssue(String type, String description, int lineNumber,
                        String vulnerableCode, String recommendation,
                        SecuritySeverity severity) {
        this.type = type;
        this.description = description;
        this.lineNumber = lineNumber;
        this.vulnerableCode = vulnerableCode;
        this.recommendation = recommendation;
        this.severity = severity;
    }
    
    public String getType() { return type; }
    public String getDescription() { return description; }
    public int getLineNumber() { return lineNumber; }
    public String getVulnerableCode() { return vulnerableCode; }
    public String getRecommendation() { return recommendation; }
    public SecuritySeverity getSeverity() { return severity; }
}