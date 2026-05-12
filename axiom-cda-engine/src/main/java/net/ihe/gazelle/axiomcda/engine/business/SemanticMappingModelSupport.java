package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;

public final class SemanticMappingModelSupport {
    private SemanticMappingModelSupport() {
    }

    public static String resolvePrimaryTargetType(SemanticMappingModel model) {
        if (model == null || model.groups().isEmpty()) {
            return null;
        }
        for (var group : model.groups()) {
            if (group.targetType() != null && !group.targetType().isBlank()) {
                return group.targetType().trim();
            }
        }
        return null;
    }
}
