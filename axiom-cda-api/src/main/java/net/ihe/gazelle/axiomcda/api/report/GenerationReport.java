package net.ihe.gazelle.axiomcda.api.report;

import java.util.List;

public record GenerationReport(
        int templatesProcessed,
        int profilesGenerated,
        int invariantsGenerated,
        int unmappedElements,
        int unresolvedValueSets,
        List<String> warnings
) {
    public GenerationReport {
        if (warnings == null) {
            throw new IllegalArgumentException("warnings must be set");
        }
    }
}
