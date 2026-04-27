package net.ihe.gazelle.axiomcda.ws.dto;

public record FshProfile(
        String name,
        String content,
        String templateId,
        String rootCdaType,
        boolean fhirTransformEligible,
        String fhirTransformKind,
        String fhirTransformNotice
) {
}
