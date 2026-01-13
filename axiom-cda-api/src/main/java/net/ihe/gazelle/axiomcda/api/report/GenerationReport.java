package net.ihe.gazelle.axiomcda.api.report;

import java.util.List;

public record GenerationReport(
        int templatesConsidered,
        int templatesGenerated,
        int templatesSkipped,
        int templatesOk,
        int profilesGenerated,
        int invariantsGenerated,
        int unmappedElements,
        int unresolvedValueSets,
        List<String> warnings,
        List<String> errors
) {
    public GenerationReport {
        if (warnings == null) {
            throw new IllegalArgumentException("warnings must be set");
        }
        if (errors == null) {
            throw new IllegalArgumentException("errors must be set");
        }
    }
}
