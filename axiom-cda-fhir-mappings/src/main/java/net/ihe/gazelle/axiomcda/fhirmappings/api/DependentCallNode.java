package net.ihe.gazelle.axiomcda.fhirmappings.api;

import java.util.List;

public record DependentCallNode(String name, List<String> variables) {

    public DependentCallNode {
        variables = variables == null ? List.of() : List.copyOf(variables);
    }
}
