package net.ihe.gazelle.axiomcda.fhirmappings.compact;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CompactObservationMapping(String kind,
                                        String id,
                                        String label,
                                        String description,
                                        String rootCdaType,
                                        String fhirRoot,
                                        String parent,
                                        Map<String, CompactObservationBranch> branches) {

    public CompactObservationMapping {
        branches = branches == null ? Map.of() : Map.copyOf(branches);
    }
}
