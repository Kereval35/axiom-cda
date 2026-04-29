package net.ihe.gazelle.axiomcda.fhirmappings.api;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class SemanticMappingModelFilter {

    private SemanticMappingModelFilter() {
    }

    public static SemanticMappingModel filterByIdentity(SemanticMappingModel model, Set<SemanticRule> includedRules) {
        if (model == null || includedRules == null || includedRules.isEmpty()) {
            return new SemanticMappingModel(List.of());
        }
        Set<SemanticRule> identitySet = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        identitySet.addAll(includedRules);
        return new SemanticMappingModel(model.groups().stream()
                .map(group -> new SemanticGroup(
                        group.name(),
                        group.rules().stream()
                                .map(rule -> filterRule(rule, identitySet))
                                .filter(filtered -> filtered != null)
                                .toList()
                ))
                .filter(group -> !group.rules().isEmpty())
                .toList());
    }

    private static SemanticRule filterRule(SemanticRule rule, Set<SemanticRule> includedRules) {
        List<SemanticRule> filteredChildren = rule.children().stream()
                .map(child -> filterRule(child, includedRules))
                .filter(filtered -> filtered != null)
                .toList();
        if (!includedRules.contains(rule) && filteredChildren.isEmpty()) {
            return null;
        }
        return new SemanticRule(
                rule.groupName(),
                rule.name(),
                rule.depth(),
                rule.conditional(),
                rule.mappingKind(),
                rule.displaySourceLabel(),
                rule.primarySourcePath(),
                rule.branchLineage(),
                rule.sources(),
                rule.targets(),
                rule.dependentCalls(),
                filteredChildren
        );
    }
}
