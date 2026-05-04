package net.ihe.gazelle.axiomcda.fhirmappings.compact;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ObservationMappingBranch(String target,
                                       String cardinalityFrom,
                                       String transform,
                                       String terminologyMap,
                                       String onlyType,
                                       String projection,
                                       Map<String, String> fields,
                                       Map<String, String> typeMap,
                                       ObservationMappingRelationship relationship,
                                       Map<String, ObservationMappingFlavor> flavors,
                                       List<String> notes) {

    public ObservationMappingBranch {
        fields = fields == null ? Map.of() : Map.copyOf(fields);
        typeMap = typeMap == null ? Map.of() : Map.copyOf(typeMap);
        flavors = flavors == null ? Map.of() : Map.copyOf(flavors);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
