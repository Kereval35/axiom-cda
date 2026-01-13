package net.ihe.gazelle.axiomcda.ws.dto;

import net.ihe.gazelle.axiomcda.api.report.GenerationReport;

public record GenerationResult(
        String zipBase64,
        GenerationReport report
) {
}
