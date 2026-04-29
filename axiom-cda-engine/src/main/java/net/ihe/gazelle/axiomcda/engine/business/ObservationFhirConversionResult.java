package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;

import java.util.List;

public record ObservationFhirConversionResult(
        String profileName,
        String fsh,
        List<String> diagnostics,
        SemanticMappingModel usedMappingModel
) {
    public ObservationFhirConversionResult {
        if (diagnostics == null) {
            diagnostics = List.of();
        }
    }
}
