package net.ihe.gazelle.axiomcda.ws.dto;

public record FshProfile(
        String name,
        String content,
        String templateId,
        String rootCdaType,
        String templateOrigin,
        String ownershipStatus,
        String selectionReason,
        boolean fhirTransformEligible,
        String fhirTransformKind,
        String fhirTransformNotice
) {
}
