package net.ihe.gazelle.axiomcda.fhirmappings.structuremap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ihe.gazelle.axiomcda.fhirmappings.api.DependentCallNode;
import net.ihe.gazelle.axiomcda.fhirmappings.api.MappingKind;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticGroup;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModelEnricher;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticRule;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SourceNode;
import net.ihe.gazelle.axiomcda.fhirmappings.api.TargetNode;
import net.ihe.gazelle.axiomcda.fhirmappings.api.TargetParameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StructureMapSemanticAnalyzer {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> ROOT_GROUPS = List.of(
            "CdaLaboratoryObservationToFhirObservation",
            "CdaLaboratoryObservationValueToFhirObservationValue"
    );

    public SemanticMappingModel analyze(String structureMapJson) throws IOException {
        JsonNode root = MAPPER.readTree(structureMapJson);
        Map<String, JsonNode> groupsByName = new LinkedHashMap<>();
        for (JsonNode group : iterable(root.path("group"))) {
            String name = text(group, "name");
            if (name != null && !name.isBlank()) {
                groupsByName.put(name, group);
            }
        }

        List<SemanticGroup> groups = new ArrayList<>();
        Set<String> visitedRoots = new LinkedHashSet<>();
        for (String groupName : ROOT_GROUPS) {
            JsonNode group = groupsByName.get(groupName);
            if (group == null || !visitedRoots.add(groupName)) {
                continue;
            }
            AnalysisContext context = initializeContext(group);
            List<SemanticRule> rules = analyzeRules(groupName, groupsByName, group.path("rule"), context, false, 0, new LinkedHashSet<>());
            groups.add(new SemanticGroup(groupName, rules));
        }
        return SemanticMappingModelEnricher.enrich(new SemanticMappingModel(groups));
    }

    private AnalysisContext initializeContext(JsonNode group) {
        Map<String, String> sourceVars = new LinkedHashMap<>();
        Map<String, String> targetVars = new LinkedHashMap<>();
        List<GroupInput> inputs = parseInputs(group.path("input"));
        for (GroupInput input : inputs) {
            if ("source".equals(input.mode())) {
                sourceVars.put(input.name(), initialSourceRoot(input.name()));
            } else if ("target".equals(input.mode())) {
                targetVars.put(input.name(), initialTargetRoot(input.name()));
            }
        }
        return new AnalysisContext(sourceVars, targetVars, null, List.of());
    }

    private List<GroupInput> parseInputs(JsonNode inputs) {
        List<GroupInput> result = new ArrayList<>();
        for (JsonNode input : iterable(inputs)) {
            String name = text(input, "name");
            String mode = text(input, "mode");
            if (name != null && mode != null) {
                result.add(new GroupInput(name, mode));
            }
        }
        return result;
    }

    private List<SemanticRule> analyzeRules(String groupName,
                                            Map<String, JsonNode> groupsByName,
                                            JsonNode rulesNode,
                                            AnalysisContext context,
                                            boolean inheritedConditional,
                                            int depth,
                                            Set<String> callStack) {
        List<SemanticRule> rules = new ArrayList<>();
        if (!rulesNode.isArray()) {
            return rules;
        }
        for (JsonNode ruleNode : iterable(rulesNode)) {
            Map<String, String> localSourceVars = new LinkedHashMap<>(context.sourceVars());
            Map<String, String> localTargetVars = new LinkedHashMap<>(context.targetVars());
            List<SourceNode> sources = new ArrayList<>();
            List<TargetNode> targets = new ArrayList<>();
            List<DependentCallNode> dependentCalls = new ArrayList<>();
            List<SemanticRule> children = new ArrayList<>();

            boolean conditional = inheritedConditional;
            String primarySourcePath = context.currentSourcePath();
            List<String> lineage = new ArrayList<>(context.branchLineage());

            for (JsonNode source : iterable(ruleNode.path("source"))) {
                String resolvedSourcePath = resolveSourcePath(localSourceVars, source);
                String variable = text(source, "variable");
                String condition = text(source, "condition");
                String sourceType = normalizeType(text(source, "type"));
                if (condition != null && !condition.isBlank()) {
                    conditional = true;
                }
                if (resolvedSourcePath != null && !resolvedSourcePath.isBlank()) {
                    primarySourcePath = resolvedSourcePath;
                }
                if (variable != null && resolvedSourcePath != null) {
                    localSourceVars.put(variable, resolvedSourcePath);
                }
                sources.add(new SourceNode(
                        normalizePath(resolvedSourcePath),
                        sourceType,
                        variable,
                        condition,
                        conditional
                ));
            }

            if (primarySourcePath != null && !primarySourcePath.isBlank()) {
                lineage = extendLineage(context.branchLineage(), rootSegment(primarySourcePath));
            }

            for (JsonNode target : iterable(ruleNode.path("target"))) {
                String resolvedTargetPath = resolveTargetPath(localTargetVars, target);
                String variable = text(target, "variable");
                String transform = text(target, "transform");
                List<TargetParameter> parameters = extractParameters(target.path("parameter"));
                String createdType = extractCreatedType(transform, parameters);
                String constantValue = extractConstantValue(transform, parameters);

                if (variable != null && resolvedTargetPath != null) {
                    localTargetVars.put(variable, resolvedTargetPath);
                }
                targets.add(new TargetNode(
                        normalizePath(resolvedTargetPath),
                        variable,
                        transform == null ? "" : transform,
                        constantValue,
                        createdType,
                        parameters,
                        conditional
                ));
            }

            AnalysisContext childContext = new AnalysisContext(localSourceVars, localTargetVars, primarySourcePath, lineage);
            children.addAll(analyzeRules(groupName, groupsByName, ruleNode.path("rule"), childContext, conditional, depth + 1, callStack));

            for (JsonNode dependent : iterable(ruleNode.path("dependent"))) {
                String dependentName = text(dependent, "name");
                List<String> arguments = new ArrayList<>();
                for (JsonNode variable : iterable(dependent.path("variable"))) {
                    if (!variable.isNull()) {
                        arguments.add(variable.asText());
                    }
                }
                DependentCallNode call = new DependentCallNode(dependentName == null ? "" : dependentName, arguments);
                dependentCalls.add(call);

                if (dependentName == null || dependentName.isBlank() || callStack.contains(dependentName)) {
                    continue;
                }
                JsonNode dependentGroup = groupsByName.get(dependentName);
                if (dependentGroup == null) {
                    continue;
                }
                AnalysisContext dependentContext = bindDependentInputs(dependentGroup, arguments, localSourceVars, localTargetVars, primarySourcePath, lineage);
                Set<String> nestedCallStack = new LinkedHashSet<>(callStack);
                nestedCallStack.add(dependentName);
                children.addAll(analyzeRules(
                        dependentName,
                        groupsByName,
                        dependentGroup.path("rule"),
                        dependentContext,
                        conditional,
                        depth + 1,
                        nestedCallStack
                ));
            }

            rules.add(new SemanticRule(
                    groupName,
                    text(ruleNode, "name"),
                    depth,
                    conditional,
                    null,
                    null,
                    normalizePath(primarySourcePath),
                    lineage,
                    sources,
                    targets,
                    dependentCalls,
                    children
            ));
        }
        return rules;
    }

    private AnalysisContext bindDependentInputs(JsonNode group,
                                                List<String> arguments,
                                                Map<String, String> localSourceVars,
                                                Map<String, String> localTargetVars,
                                                String currentSourcePath,
                                                List<String> lineage) {
        Map<String, String> sourceVars = new LinkedHashMap<>();
        Map<String, String> targetVars = new LinkedHashMap<>();
        List<GroupInput> inputs = parseInputs(group.path("input"));
        for (int i = 0; i < inputs.size(); i++) {
            GroupInput input = inputs.get(i);
            String argument = i < arguments.size() ? arguments.get(i) : null;
            String resolved = null;
            if (argument != null) {
                resolved = "source".equals(input.mode())
                        ? localSourceVars.get(argument)
                        : localTargetVars.get(argument);
                if (resolved == null) {
                    resolved = argument;
                }
            }
            if ("source".equals(input.mode())) {
                sourceVars.put(input.name(), resolved == null ? initialSourceRoot(input.name()) : resolved);
            } else if ("target".equals(input.mode())) {
                targetVars.put(input.name(), resolved == null ? initialTargetRoot(input.name()) : resolved);
            }
        }
        return new AnalysisContext(sourceVars, targetVars, currentSourcePath, lineage);
    }

    private String resolveSourcePath(Map<String, String> sourceVars, JsonNode source) {
        String context = text(source, "context");
        String base = context == null ? "" : sourceVars.getOrDefault(context, "");
        String element = text(source, "element");
        if (element == null || element.isBlank()) {
            return base;
        }
        return join(base, element);
    }

    private String resolveTargetPath(Map<String, String> targetVars, JsonNode target) {
        String context = text(target, "context");
        String base = context == null ? "" : targetVars.getOrDefault(context, "");
        String element = text(target, "element");
        if (element == null || element.isBlank()) {
            return base;
        }
        return join(base, element);
    }

    private List<TargetParameter> extractParameters(JsonNode parameters) {
        List<TargetParameter> values = new ArrayList<>();
        if (!parameters.isArray()) {
            return values;
        }
        for (JsonNode parameter : iterable(parameters)) {
            if (parameter.hasNonNull("valueString")) {
                values.add(new TargetParameter("string", parameter.get("valueString").asText()));
            } else if (parameter.hasNonNull("valueId")) {
                values.add(new TargetParameter("id", parameter.get("valueId").asText()));
            } else if (parameter.hasNonNull("valueCode")) {
                values.add(new TargetParameter("code", parameter.get("valueCode").asText()));
            }
        }
        return values;
    }

    private String extractCreatedType(String transform, List<TargetParameter> parameters) {
        if (!"create".equals(transform) || parameters.isEmpty()) {
            return null;
        }
        return parameters.get(0).value();
    }

    private String extractConstantValue(String transform, List<TargetParameter> parameters) {
        if (parameters.size() != 1) {
            return null;
        }
        TargetParameter parameter = parameters.get(0);
        if (!"string".equals(parameter.kind()) && !"code".equals(parameter.kind())) {
            return null;
        }
        return switch (transform == null ? "" : transform) {
            case "copy", "create" -> parameter.value();
            default -> null;
        };
    }

    private List<String> extendLineage(List<String> current, String segment) {
        if (segment == null || segment.isBlank()) {
            return current == null ? List.of() : current;
        }
        List<String> result = new ArrayList<>(current == null ? List.of() : current);
        if (result.isEmpty() || !segment.equals(result.get(result.size() - 1))) {
            result.add(segment);
        }
        return result;
    }

    private String initialSourceRoot(String name) {
        return "";
    }

    private String initialTargetRoot(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("observation")) {
            return "";
        }
        if (lower.contains("bundle")) {
            return "@bundle";
        }
        if (lower.contains("patient")) {
            return "@patient";
        }
        if (lower.contains("specimen")) {
            return "@specimen";
        }
        return "@" + name;
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

    private String rootSegment(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        int dot = path.indexOf('.');
        return dot >= 0 ? path.substring(0, dot) : path;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Iterable<JsonNode> iterable(JsonNode node) {
        return node::elements;
    }

    private record GroupInput(String name, String mode) {
    }

    private record AnalysisContext(Map<String, String> sourceVars,
                                   Map<String, String> targetVars,
                                   String currentSourcePath,
                                   List<String> branchLineage) {
    }
}
