package net.ihe.gazelle.axiomcda.fhirmappings.api;

import java.util.List;

public record SemanticGroup(String name, List<SemanticRule> rules) {

    public SemanticGroup {
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    void flattenInto(List<SemanticRule> destination) {
        for (SemanticRule rule : rules) {
            destination.add(rule);
            rule.flattenInto(destination);
        }
    }
}
