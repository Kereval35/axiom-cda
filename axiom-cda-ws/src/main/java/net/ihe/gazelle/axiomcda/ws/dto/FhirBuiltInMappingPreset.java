package net.ihe.gazelle.axiomcda.ws.dto;

public record FhirBuiltInMappingPreset(
        String id,
        String label,
        String description,
        String rootCdaType,
        String family,
        boolean defaultSelected
) {
}
