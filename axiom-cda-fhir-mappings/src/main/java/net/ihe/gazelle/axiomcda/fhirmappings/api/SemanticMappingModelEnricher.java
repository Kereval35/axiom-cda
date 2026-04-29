package net.ihe.gazelle.axiomcda.fhirmappings.api;

import java.util.List;

public final class SemanticMappingModelEnricher {

    private SemanticMappingModelEnricher() {
    }

    public static SemanticMappingModel enrich(SemanticMappingModel model) {
        if (model == null) {
            return new SemanticMappingModel(List.of());
        }
        return new SemanticMappingModel(model.groups().stream()
                .map(group -> new SemanticGroup(
                        group.name(),
                        group.rules().stream().map(SemanticMappingModelEnricher::enrichRule).toList()
                ))
                .toList());
    }

    private static SemanticRule enrichRule(SemanticRule rule) {
        MappingKind mappingKind = rule.mappingKind() != null ? rule.mappingKind() : inferKind(rule);
        String displaySourceLabel = rule.displaySourceLabel() != null && !rule.displaySourceLabel().isBlank()
                ? rule.displaySourceLabel()
                : inferDisplaySourceLabel(rule, mappingKind);
        return new SemanticRule(
                rule.groupName(),
                rule.name(),
                rule.depth(),
                rule.conditional(),
                mappingKind,
                displaySourceLabel,
                rule.primarySourcePath(),
                rule.branchLineage(),
                rule.sources(),
                rule.targets(),
                rule.dependentCalls(),
                rule.children().stream().map(SemanticMappingModelEnricher::enrichRule).toList()
        );
    }

    private static MappingKind inferKind(SemanticRule rule) {
        boolean hasConcreteSource = rule.sources().stream()
                .map(SourceNode::path)
                .anyMatch(SemanticMappingModelEnricher::hasText);
        boolean hasTargets = rule.targets().stream()
                .map(TargetNode::path)
                .anyMatch(SemanticMappingModelEnricher::hasText);
        boolean hasChildren = !rule.children().isEmpty();
        boolean hasDependentCalls = !rule.dependentCalls().isEmpty();
        boolean hasConstants = rule.targets().stream()
                .anyMatch(target -> hasText(target.constantValue())
                        || hasText(target.createdType())
                        || (hasText(target.transform()) && !"copy".equals(target.transform())));

        if (hasConcreteSource) {
            return MappingKind.DIRECT_PATH;
        }
        if (!hasTargets && (hasChildren || hasDependentCalls)) {
            return MappingKind.HELPER_ONLY;
        }
        if (hasConstants || rule.sources().stream().allMatch(source -> !hasText(source.path()))) {
            return MappingKind.GLOBAL_CONSTANT;
        }
        return MappingKind.CONTEXT_DERIVED;
    }

    private static String inferDisplaySourceLabel(SemanticRule rule, MappingKind kind) {
        return switch (kind) {
            case DIRECT_PATH -> firstConcreteSourcePath(rule);
            case CONTEXT_DERIVED -> "CONTEXT";
            case GLOBAL_CONSTANT -> "GLOBAL";
            case HELPER_ONLY -> "HELPER";
        };
    }

    private static String firstConcreteSourcePath(SemanticRule rule) {
        return rule.sources().stream()
                .map(SourceNode::path)
                .filter(SemanticMappingModelEnricher::hasText)
                .findFirst()
                .orElseGet(() -> hasText(rule.primarySourcePath()) ? rule.primarySourcePath() : "CONTEXT");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
