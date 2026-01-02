package net.ihe.gazelle.axiomcda.api.ir;

public record IRInvariant(String name, String description, IRInvariantSeverity severity, String expression) {
    public IRInvariant {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must be set");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity must be set");
        }
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("expression must be set");
        }
    }
}
