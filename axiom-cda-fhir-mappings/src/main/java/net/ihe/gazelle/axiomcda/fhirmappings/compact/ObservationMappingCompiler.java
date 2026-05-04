package net.ihe.gazelle.axiomcda.fhirmappings.compact;

import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticGroup;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModelEnricher;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticRule;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SourceNode;
import net.ihe.gazelle.axiomcda.fhirmappings.api.TargetNode;
import net.ihe.gazelle.axiomcda.fhirmappings.api.TargetParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ObservationMappingCompiler {

    public SemanticMappingModel compile(ObservationMapping mapping) {
        if (mapping == null) {
            throw new IllegalArgumentException("mapping must be set");
        }
        String groupName = groupName(mapping);
        List<SemanticRule> rules = new ArrayList<>();
        addParentRule(groupName, mapping.parent(), rules);
        for (ObservationMappingFixedTarget fixedTarget : mapping.fixedTargets()) {
            addFixedTargetRule(groupName, fixedTarget, rules);
        }
        for (Map.Entry<String, ObservationMappingBranch> entry : mapping.branches().entrySet()) {
            compileBranch(groupName, entry.getKey(), entry.getValue(), rules);
        }
        return SemanticMappingModelEnricher.enrich(new SemanticMappingModel(List.of(new SemanticGroup(groupName, rules))));
    }

    private void compileBranch(String groupName,
                               String branchName,
                               ObservationMappingBranch branch,
                               List<SemanticRule> rules) {
        if (branch == null) {
            return;
        }
        if (!branch.flavors().isEmpty()) {
            for (Map.Entry<String, ObservationMappingFlavor> flavorEntry : branch.flavors().entrySet()) {
                compileFlavor(groupName, branchName, flavorEntry.getKey(), flavorEntry.getValue(), rules);
            }
        }
        if (branch.target() != null && !branch.target().isBlank() && branch.fields().isEmpty()) {
            maybeAddSelfRule(groupName, branchName, branch.target(), branch.relationship(), branch.onlyType(), branch.transform(), branch.terminologyMap(), rules);
        }
        for (Map.Entry<String, String> field : branch.fields().entrySet()) {
            addFieldRule(groupName, branchName, branch.target(), field.getKey(), field.getValue(), branch.relationship(), branch.transform(), branch.terminologyMap(), rules);
        }
        for (Map.Entry<String, String> typeEntry : branch.typeMap().entrySet()) {
            addTypeRule(groupName, branchName, typeEntry.getKey(), typeEntry.getValue(), rules);
        }
    }

    private void compileFlavor(String groupName,
                               String branchName,
                               String flavorName,
                               ObservationMappingFlavor flavor,
                               List<SemanticRule> rules) {
        if (flavor == null) {
            return;
        }
        String sourceRoot = branchName + "." + flavorName;
        if (flavor.target() != null && !flavor.target().isBlank() && flavor.fields().isEmpty()) {
            maybeAddSelfRule(groupName, sourceRoot, flavor.target(), flavor.relationship(), null, null, null, rules);
        }
        for (Map.Entry<String, String> field : flavor.fields().entrySet()) {
            addFieldRule(groupName, sourceRoot, flavor.target(), field.getKey(), field.getValue(), flavor.relationship(), null, null, rules);
        }
    }

    private void maybeAddSelfRule(String groupName,
                                  String sourceRoot,
                                  String target,
                                  ObservationMappingRelationship relationship,
                                  String onlyType,
                                  String transform,
                                  String terminologyMap,
                                  List<SemanticRule> rules) {
        String createdType = firstNonBlank(
                relationship != null ? relationship.createdType() : null,
                onlyType
        );
        String resolvedTransform = createdType != null ? "create" : resolveTransform(relationship, transform);
        addRule(groupName,
                ruleName(sourceRoot, null, "self"),
                sourceRoot,
                normalizeSemanticTargetPath(target),
                null,
                createdType,
                createdType,
                resolvedTransform,
                parametersFor(resolvedTransform, terminologyMap, createdType),
                rules);
    }

    private void addFieldRule(String groupName,
                              String sourceRoot,
                              String baseTarget,
                              String fieldKey,
                              String targetSuffix,
                              ObservationMappingRelationship relationship,
                              String transform,
                              String terminologyMap,
                              List<SemanticRule> rules) {
        if (fieldKey == null || targetSuffix == null || targetSuffix.isBlank()) {
            return;
        }
        String sourcePath = "self".equals(fieldKey) ? sourceRoot : sourceRoot + "." + fieldKey;
        String targetPath = resolveTargetPath(baseTarget, targetSuffix);
        String createdType = relationship != null && relationship.createdType() != null && "self".equals(fieldKey)
                ? relationship.createdType()
                : null;
        String resolvedTransform = createdType != null ? "create" : resolveTransform(null, transform);
        addRule(groupName,
                ruleName(sourceRoot, baseTarget, fieldKey),
                sourcePath,
                targetPath,
                null,
                createdType,
                createdType,
                resolvedTransform,
                parametersFor(resolvedTransform, terminologyMap, createdType),
                rules);
    }

    private void addTypeRule(String groupName,
                             String sourceRoot,
                             String sourceType,
                             String fhirType,
                             List<SemanticRule> rules) {
        if (sourceType == null || sourceType.isBlank() || fhirType == null || fhirType.isBlank()) {
            return;
        }
        addRule(groupName,
                ruleName(sourceRoot, "value", sourceType),
                sourceRoot,
                "value",
                sourceType,
                fhirType,
                fhirType,
                "create",
                List.of(new TargetParameter("string", fhirType)),
                rules);
    }

    private void addRule(String groupName,
                         String name,
                         String sourcePath,
                         String targetPath,
                         String sourceType,
                         String constantValue,
                         String createdType,
                         String transform,
                         List<TargetParameter> parameters,
                         List<SemanticRule> rules) {
        List<SourceNode> sources = List.of(new SourceNode(sourcePath, sourceType, sourceVariable(sourcePath), null, false));
        String normalizedTargetPath = normalizeSemanticTargetPath(targetPath);
        List<TargetNode> targets = List.of(new TargetNode(normalizedTargetPath,
                targetVariable(normalizedTargetPath),
                transform == null ? "" : transform,
                constantValue,
                createdType,
                parameters == null ? List.of() : parameters,
                false));
        rules.add(new SemanticRule(
                groupName,
                name,
                0,
                false,
                null,
                sourcePath,
                sourcePath,
                sourcePath == null || sourcePath.isBlank() ? List.of() : List.of(rootSegment(sourcePath)),
                sources,
                targets,
                List.of(),
                List.of()
        ));
    }

    private void addParentRule(String groupName,
                               String parent,
                               List<SemanticRule> rules) {
        if (parent == null || parent.isBlank()) {
            return;
        }
        addRule(groupName,
                "ObservationParentProfile",
                "",
                "meta.profile",
                null,
                parent,
                null,
                "",
                List.of(),
                rules);
    }

    private void addFixedTargetRule(String groupName,
                                    ObservationMappingFixedTarget fixedTarget,
                                    List<SemanticRule> rules) {
        if (fixedTarget == null || fixedTarget.target() == null || fixedTarget.target().isBlank()) {
            return;
        }
        String createdType = fixedTarget.createdType();
        String transform = fixedTarget.transform() == null ? "copy" : fixedTarget.transform();
        addRule(groupName,
                ruleName("global", normalizeSemanticTargetPath(fixedTarget.target()), "fixed"),
                "",
                fixedTarget.target(),
                null,
                fixedTarget.value(),
                createdType,
                transform,
                parametersFor(transform, null, createdType, fixedTarget.value()),
                rules);
    }

    private String resolveTargetPath(String baseTarget, String targetSuffix) {
        if (targetSuffix == null || targetSuffix.isBlank()) {
            return "";
        }
        if ("self".equals(targetSuffix)) {
            return normalizeSemanticTargetPath(baseTarget);
        }
        String normalizedBase = normalizeSemanticTargetPath(baseTarget);
        if (normalizedBase == null || normalizedBase.isBlank()) {
            return normalizeSemanticTargetPath(targetSuffix);
        }
        String normalizedSuffix = normalizeSemanticTargetPath(targetSuffix);
        if (normalizedSuffix.equals(normalizedBase)
                || normalizedSuffix.startsWith(normalizedBase + ".")
                || normalizedSuffix.startsWith("@")) {
            return normalizedSuffix;
        }
        if (normalizedSuffix.startsWith(".")) {
            return normalizedBase + normalizedSuffix;
        }
        return normalizedBase + "." + normalizedSuffix;
    }

    private String groupName(ObservationMapping mapping) {
        String root = mapping.rootCdaType() == null || mapping.rootCdaType().isBlank()
                ? "Observation"
                : mapping.rootCdaType().trim();
        return "Cda" + toPascalCase(root) + "ToFhir" + toPascalCase(mapping.fhirRoot());
    }

    private String ruleName(String branch, String baseTarget, String suffix) {
        if ("self".equals(suffix) && baseTarget != null && !baseTarget.isBlank()) {
            return toPascalCase(branch) + "To" + toPascalCase(normalizeSemanticTargetPath(baseTarget));
        }
        return toPascalCase(branch) + toPascalCase(suffix);
    }

    private String sourceVariable(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return "cda_" + path.replace('.', '_');
    }

    private String targetVariable(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return "fhir_" + normalizeSemanticTargetPath(path).replace('.', '_').replace("[x]", "");
    }

    private String rootSegment(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        int dot = path.indexOf('.');
        return dot >= 0 ? path.substring(0, dot) : path;
    }

    private String normalizeSemanticTargetPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = path.trim();
        if (normalized.endsWith("[x]")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        return normalized;
    }

    private String resolveTransform(ObservationMappingRelationship relationship, String declaredTransform) {
        if (relationship != null && relationship.createdType() != null) {
            return "create";
        }
        if (declaredTransform != null && !declaredTransform.isBlank()) {
            return declaredTransform;
        }
        return "copy";
    }

    private List<TargetParameter> parametersFor(String transform,
                                                String terminologyMap,
                                                String createdType) {
        return parametersFor(transform, terminologyMap, createdType, null);
    }

    private List<TargetParameter> parametersFor(String transform,
                                                String terminologyMap,
                                                String createdType,
                                                String constantValue) {
        if ("translate".equals(transform) && terminologyMap != null && !terminologyMap.isBlank()) {
            return List.of(
                    new TargetParameter("string", "#" + terminologyMap),
                    new TargetParameter("string", "code")
            );
        }
        if ("create".equals(transform) && createdType != null && !createdType.isBlank()) {
            return List.of(new TargetParameter("string", createdType));
        }
        if ("copy".equals(transform) && constantValue != null && !constantValue.isBlank()) {
            return List.of(new TargetParameter("string", constantValue));
        }
        return List.of();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String toPascalCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String[] parts = value.split("[^A-Za-z0-9]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }
}
