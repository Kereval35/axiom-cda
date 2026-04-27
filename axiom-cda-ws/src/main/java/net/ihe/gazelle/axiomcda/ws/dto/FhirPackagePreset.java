package net.ihe.gazelle.axiomcda.ws.dto;

public record FhirPackagePreset(
        String label,
        String packageId,
        String version,
        String description
) {
}
