package net.ihe.gazelle.axiomcda.fhirmappings.api;

import java.util.ArrayList;
import java.util.List;

public record SemanticMappingModel(List<SemanticGroup> groups) {

    public SemanticMappingModel {
        groups = groups == null ? List.of() : List.copyOf(groups);
    }

    public List<SemanticRule> allRules() {
        List<SemanticRule> rules = new ArrayList<>();
        for (SemanticGroup group : groups) {
            group.flattenInto(rules);
        }
        return List.copyOf(rules);
    }
}
