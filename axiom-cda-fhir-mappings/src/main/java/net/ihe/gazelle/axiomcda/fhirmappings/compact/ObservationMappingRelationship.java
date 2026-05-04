package net.ihe.gazelle.axiomcda.fhirmappings.compact;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ObservationMappingRelationship(String kind, String createdType) {
}
