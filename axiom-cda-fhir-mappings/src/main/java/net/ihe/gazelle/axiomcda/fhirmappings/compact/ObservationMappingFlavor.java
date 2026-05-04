package net.ihe.gazelle.axiomcda.fhirmappings.compact;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ObservationMappingFlavor(String target,
                                       String projection,
                                       Map<String, String> fields,
                                       ObservationMappingRelationship relationship,
                                       List<String> ignore,
                                       List<String> notes) {

    public ObservationMappingFlavor {
        fields = fields == null ? Map.of() : Map.copyOf(fields);
        ignore = ignore == null ? List.of() : List.copyOf(ignore);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
