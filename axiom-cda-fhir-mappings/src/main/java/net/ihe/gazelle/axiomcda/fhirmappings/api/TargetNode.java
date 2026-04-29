package net.ihe.gazelle.axiomcda.fhirmappings.api;

import java.util.List;

public record TargetNode(String path,
                         String variable,
                         String transform,
                         String constantValue,
                         String createdType,
                         List<TargetParameter> parameters,
                         boolean conditional) {

    public TargetNode {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }
}
