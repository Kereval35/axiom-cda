package net.ihe.gazelle.axiomcda.api.ir;

public record IRDiagnostic(IRDiagnosticSeverity severity, String templateId, String path, String message) {
    public IRDiagnostic {
        if (severity == null) {
            throw new IllegalArgumentException("severity must be set");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must be set");
        }
    }
}
