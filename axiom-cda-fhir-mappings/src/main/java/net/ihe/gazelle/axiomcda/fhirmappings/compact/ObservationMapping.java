package net.ihe.gazelle.axiomcda.fhirmappings.compact;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ObservationMapping(String kind,
                                 String id,
                                 String label,
                                 String description,
                                 String rootCdaType,
                                 String fhirRoot,
                                 String parent,
                                 List<ObservationMappingFixedTarget> fixedTargets,
                                 Map<String, ObservationMappingBranch> branches) {

    public ObservationMapping {
        fixedTargets = fixedTargets == null ? List.of() : List.copyOf(fixedTargets);
        branches = branches == null ? Map.of() : Map.copyOf(branches);
    }
}
