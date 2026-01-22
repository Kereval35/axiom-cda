package net.ihe.gazelle.axiomcda.ws.dto;

import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.api.report.GenerationReport;

import java.util.List;

public record GenerationResult(
        String zipBase64,
        GenerationReport report,
        List<FshProfile> profiles,
        List<IRTemplate> irTemplates
) {
    public GenerationResult {
        if (profiles == null) {
            profiles = List.of();
        }
        if (irTemplates == null) {
            irTemplates = List.of();
        }
    }
}
