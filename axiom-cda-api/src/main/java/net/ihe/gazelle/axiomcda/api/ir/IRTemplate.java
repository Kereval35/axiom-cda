package net.ihe.gazelle.axiomcda.api.ir;

import java.util.List;

public record IRTemplate(
        String id,
        String name,
        String displayName,
        String description,
        String rootCdaType,
        List<IRElementConstraint> elements,
        List<IRTemplateInclude> includes,
        List<IRInvariant> invariants
) {
    public IRTemplate {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must be set");
        }
        if (rootCdaType == null || rootCdaType.isBlank()) {
            throw new IllegalArgumentException("rootCdaType must be set");
        }
    }
}
