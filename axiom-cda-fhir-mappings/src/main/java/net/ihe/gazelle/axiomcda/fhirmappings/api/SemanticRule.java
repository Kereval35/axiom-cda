package net.ihe.gazelle.axiomcda.fhirmappings.api;

import java.util.List;

public record SemanticRule(String groupName,
                           String name,
                           int depth,
                           boolean conditional,
                           MappingKind mappingKind,
                           String displaySourceLabel,
                           String primarySourcePath,
                           List<String> branchLineage,
                           List<SourceNode> sources,
                           List<TargetNode> targets,
                           List<DependentCallNode> dependentCalls,
                           List<SemanticRule> children) {

    public SemanticRule {
        branchLineage = branchLineage == null ? List.of() : List.copyOf(branchLineage);
        sources = sources == null ? List.of() : List.copyOf(sources);
        targets = targets == null ? List.of() : List.copyOf(targets);
        dependentCalls = dependentCalls == null ? List.of() : List.copyOf(dependentCalls);
        children = children == null ? List.of() : List.copyOf(children);
    }

    void flattenInto(List<SemanticRule> destination) {
        for (SemanticRule child : children) {
            destination.add(child);
            child.flattenInto(destination);
        }
    }
}
