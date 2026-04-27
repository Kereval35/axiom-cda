package net.ihe.gazelle.axiomcda.engine.business;

import java.util.List;

public record ObservationFhirConversionResult(
        String profileName,
        String fsh,
        List<String> diagnostics
) {
    public ObservationFhirConversionResult {
        if (diagnostics == null) {
            diagnostics = List.of();
        }
    }
}
