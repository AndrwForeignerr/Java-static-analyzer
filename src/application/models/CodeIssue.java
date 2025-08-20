package application.models;

public class CodeIssue {
    
    private final int lineNumber;
    private final IssueType type;
    private final IssueSeverity severity;
    private final String description;
    
    public CodeIssue(int lineNumber, IssueType type, IssueSeverity severity, String description) {
        this.lineNumber = lineNumber;
        this.type = type;
        this.severity = severity;
        this.description = description;
    }
    
    public int getLineNumber() { return lineNumber; }
    public IssueType getType() { return type; }
    public IssueSeverity getSeverity() { return severity; }
    public String getDescription() { return description; }
}