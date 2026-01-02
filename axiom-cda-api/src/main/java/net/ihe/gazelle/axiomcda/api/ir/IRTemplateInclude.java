package net.ihe.gazelle.axiomcda.api.ir;

public record IRTemplateInclude(String path, String templateId, IRCardinality cardinality) {
    public IRTemplateInclude {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must be set");
        }
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId must be set");
        }
    }
}
