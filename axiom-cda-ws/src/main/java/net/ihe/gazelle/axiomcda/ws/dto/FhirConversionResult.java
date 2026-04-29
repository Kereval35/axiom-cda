package net.ihe.gazelle.axiomcda.ws.dto;

import java.util.List;

public record FhirConversionResult(
        List<FshProfile> profiles,
        List<String> diagnostics,
        String mappingRulesName,
        String mappingRulesFsh,
        String usedMappingRulesName,
        String usedMappingRulesFsh
) {
    public FhirConversionResult {
        if (profiles == null) {
            profiles = List.of();
        }
        if (diagnostics == null) {
            diagnostics = List.of();
        }
    }
}
