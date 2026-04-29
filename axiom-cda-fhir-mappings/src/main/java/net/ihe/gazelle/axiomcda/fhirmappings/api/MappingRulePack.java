package net.ihe.gazelle.axiomcda.fhirmappings.api;

public record MappingRulePack(String id,
                              String label,
                              String description,
                              String rootCdaType,
                              String fhirVersion,
                              String family,
                              String version,
                              String status,
                              SemanticMappingModel model) {
}
