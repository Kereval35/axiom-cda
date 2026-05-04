package net.ihe.gazelle.axiomcda.fhirmappings.compact;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ObservationMappingFixedTarget(String target,
                                            String value,
                                            String transform,
                                            String createdType) {
}
