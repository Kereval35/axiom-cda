package net.ihe.gazelle.axiomcda.engine.business;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;

class ObservationStructureMapFlattener {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> SUPPORTED_GROUPS = Set.of(
            "CdaLaboratoryObservationToFhirObservation",
            "CdaLaboratoryObservationValueToFhirObservationValue"
    );

    List<FlattenedStructureMapOperation> flatten(String structureMapJson) throws IOException {
        JsonNode root = MAPPER.readTree(structureMapJson);
        List<FlattenedStructureMapOperation> operations = new ArrayList<>();
        JsonNode groups = root.path("group");
        if (!groups.isArray()) {
            return operations;
        }
        for (JsonNode group : groups) {
            String groupName = group.path("name").asText();
            if (!SUPPORTED_GROUPS.contains(groupName)) {
                continue;
            }
            Map<String, String> sourceVars = new LinkedHashMap<>();
            Map<String, String> targetVars = new LinkedHashMap<>();
            for (JsonNode input : iterable(group.path("input"))) {
                String name = text(input, "name");
                String mode = text(input, "mode");
                if (name == null || mode == null) {
                    continue;
                }
                if ("source".equals(mode)) {
                    sourceVars.put(name, "");
                } else if ("target".equals(mode)) {
                    targetVars.put(name, "");
                }
            }
            flattenRules(groupName, group.path("rule"), sourceVars, targetVars, operations, false);
        }
        return operations;
    }

    private void flattenRules(String groupName,
                              JsonNode rules,
                              Map<String, String> sourceVars,
                              Map<String, String> targetVars,
                              List<FlattenedStructureMapOperation> operations,
                              boolean inheritedConditional) {
        if (!rules.isArray()) {
            return;
        }
        for (JsonNode rule : rules) {
            Map<String, String> localSourceVars = new LinkedHashMap<>(sourceVars);
            Map<String, String> localTargetVars = new LinkedHashMap<>(targetVars);
            String ruleName = text(rule, "name");
            boolean conditional = inheritedConditional;
            String primarySourcePath = null;
            String primarySourceType = null;

            for (JsonNode source : iterable(rule.path("source"))) {
                String resolvedSourcePath = resolveSourcePath(localSourceVars, source);
                if (primarySourcePath == null && resolvedSourcePath != null) {
                    primarySourcePath = resolvedSourcePath;
                }
                if (primarySourceType == null) {
                    primarySourceType = text(source, "type");
                }
                if (source.hasNonNull("condition")) {
                    conditional = true;
                }
                String variable = text(source, "variable");
                if (variable != null && resolvedSourcePath != null) {
                    localSourceVars.put(variable, resolvedSourcePath);
                }
            }

            for (JsonNode target : iterable(rule.path("target"))) {
                String resolvedTargetPath = resolveTargetPath(localTargetVars, target);
                String transform = text(target, "transform");
                String constantValue = extractConstantValue(target.path("parameter"));
                String createdType = extractCreatedType(transform, target.path("parameter"));
                String targetVariable = text(target, "variable");

                if (targetVariable != null && resolvedTargetPath != null) {
                    localTargetVars.put(targetVariable, resolvedTargetPath);
                }
                if (resolvedTargetPath != null && !resolvedTargetPath.isBlank()) {
                    operations.add(new FlattenedStructureMapOperation(
                            groupName,
                            ruleName == null ? "" : ruleName,
                            normalizePath(primarySourcePath),
                            normalizeType(primarySourceType),
                            normalizePath(resolvedTargetPath),
                            transform == null ? "" : transform,
                            constantValue,
                            createdType,
                            conditional
                    ));
                }
            }

            flattenRules(groupName, rule.path("rule"), localSourceVars, localTargetVars, operations, conditional);
        }
    }

    private String resolveSourcePath(Map<String, String> sourceVars, JsonNode source) {
        String context = text(source, "context");
        String base = context == null ? "" : sourceVars.getOrDefault(context, "");
        String element = text(source, "element");
        if (element == null || element.isBlank()) {
            return base == null ? null : base;
        }
        return join(base, element);
    }

    private String resolveTargetPath(Map<String, String> targetVars, JsonNode target) {
        String context = text(target, "context");
        String base = context == null ? "" : targetVars.getOrDefault(context, "");
        String element = text(target, "element");
        if (element == null || element.isBlank()) {
            return base == null ? null : base;
        }
        return join(base, element);
    }

    private String extractConstantValue(JsonNode parameters) {
        if (!parameters.isArray() || parameters.size() != 1) {
            return null;
        }
        JsonNode parameter = parameters.get(0);
        if (parameter.hasNonNull("valueString")) {
            return parameter.get("valueString").asText();
        }
        return null;
    }

    private String extractCreatedType(String transform, JsonNode parameters) {
        if (!"create".equals(transform) || !parameters.isArray() || parameters.isEmpty()) {
            return null;
        }
        JsonNode parameter = parameters.get(0);
        if (parameter.hasNonNull("valueString")) {
            return parameter.get("valueString").asText();
        }
        return null;
    }

    private String join(String base, String element) {
        if (element == null || element.isBlank()) {
            return base;
        }
        if (base == null || base.isBlank()) {
            return element;
        }
        return base + "." + element;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        String normalized = path.replace("[x]", "").replace("..", ".");
        while (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return type.replace('-', '_');
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Iterable<JsonNode> iterable(JsonNode node) {
        return node::elements;
    }

    record FlattenedStructureMapOperation(
            String groupName,
            String ruleName,
            String sourcePath,
            String sourceType,
            String targetPath,
            String transform,
            String constantValue,
            String createdType,
            boolean conditional
    ) {
    }
}
