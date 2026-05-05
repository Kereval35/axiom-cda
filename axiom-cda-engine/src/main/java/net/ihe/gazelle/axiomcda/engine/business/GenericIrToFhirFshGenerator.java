package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.ir.IRBinding;
import net.ihe.gazelle.axiomcda.api.ir.IRBindingStrength;
import net.ihe.gazelle.axiomcda.api.ir.IRFixedValueType;
import net.ihe.gazelle.axiomcda.api.ir.IRElementConstraint;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.engine.util.FshUtil;
import net.ihe.gazelle.axiomcda.engine.util.NameUtil;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModelFilter;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticRule;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SourceNode;
import net.ihe.gazelle.axiomcda.fhirmappings.api.TargetNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GenericIrToFhirFshGenerator {

    private static final String DEFAULT_PARENT_PREFIX = "http://hl7.org/fhir/StructureDefinition/";

    public ObservationFhirConversionResult generate(IRTemplate template,
                                                    String sourceProfileName,
                                                    SemanticMappingModel semanticModel) throws IOException {
        if (template == null) {
            throw new IllegalArgumentException("template must be set");
        }
        if (semanticModel == null || semanticModel.allRules().isEmpty()) {
            throw new IllegalArgumentException("A StructureMap with analyzable mapping rules is required");
        }

        String parent = resolveParent(semanticModel);
        String resourceName = resourceName(parent, semanticModel, template.rootCdaType());
        String profileName = buildProfileName(sourceProfileName, template, resourceName);
        String profileId = NameUtil.toKebabCase(profileName);
        String description = template.description() == null || template.description().isBlank()
                ? "FHIR " + resourceName + " profile generated from CDA IR and uploaded StructureMap."
                : template.description();

        Map<String, List<SemanticRule>> rulesBySourcePath = indexRulesBySourcePath(semanticModel);
        Set<SemanticRule> usedRules = Collections.newSetFromMap(new IdentityHashMap<>());
        List<String> diagnostics = new ArrayList<>();
        LinkedHashSet<String> lines = new LinkedHashSet<>();
        lines.add("Profile: " + profileName);
        lines.add("Parent: " + parent);
        lines.add("Id: " + profileId);
        lines.add("Title: \"" + FshUtil.escape(profileName) + "\"");
        lines.add("Description: \"" + FshUtil.escape(description) + "\"");
        lines.add("* ^status = #draft");

        seedGlobalConstants(semanticModel, lines, usedRules);
        markParentRuleUsed(semanticModel, usedRules, parent);

        Set<String> reportedMissingRoots = new LinkedHashSet<>();
        for (IRElementConstraint element : template.elements()) {
            List<SemanticRule> matchingRules = rulesBySourcePath.getOrDefault(element.path(), List.of());
            if (matchingRules.isEmpty() && !element.path().contains(".")) {
                matchingRules = rulesBySourcePath.getOrDefault(rootSegment(element.path()), List.of());
            }
            String targetPath = resolveTargetPath(element, matchingRules);
            if (targetPath == null) {
                String root = rootSegment(element.path());
                if (reportedMissingRoots.add(root)) {
                    diagnostics.add("No safe FHIR path mapping found for CDA branch '" + root + "' in the uploaded StructureMap.");
                }
                continue;
            }
            matchingRules.forEach(usedRules::add);
            emitGenericRules(element, targetPath, lines);
        }

        return new ObservationFhirConversionResult(
                profileName,
                String.join("\n", lines) + "\n",
                diagnostics,
                SemanticMappingModelFilter.filterByIdentity(semanticModel, usedRules)
        );
    }

    private Map<String, List<SemanticRule>> indexRulesBySourcePath(SemanticMappingModel semanticModel) {
        Map<String, List<SemanticRule>> rulesBySourcePath = new LinkedHashMap<>();
        for (SemanticRule rule : semanticModel.allRules()) {
            addRuleForSourcePath(rulesBySourcePath, normalizeSourcePath(rule.primarySourcePath()), rule);
            for (SourceNode source : rule.sources()) {
                addRuleForSourcePath(rulesBySourcePath, normalizeSourcePath(source.path()), rule);
            }
        }
        return rulesBySourcePath;
    }

    private void addRuleForSourcePath(Map<String, List<SemanticRule>> rulesBySourcePath,
                                      String sourcePath,
                                      SemanticRule rule) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return;
        }
        rulesBySourcePath.computeIfAbsent(sourcePath, ignored -> new ArrayList<>()).add(rule);
    }

    private String resolveTargetPath(IRElementConstraint element, List<SemanticRule> rules) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        for (SemanticRule rule : rules) {
            if (rule.conditional()) {
                continue;
            }
            for (TargetNode target : rule.targets()) {
                String path = normalizeTargetPath(target.path());
                if (path == null || path.isBlank() || path.startsWith("@") || "meta.profile".equals(path)) {
                    continue;
                }
                candidates.add(path);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        String shortest = candidates.stream()
                .min((left, right) -> Integer.compare(left.length(), right.length()))
                .orElse(null);
        if (shortest == null) {
            return null;
        }
        if (!element.path().contains(".")) {
            return rootSegment(shortest);
        }
        return shortest;
    }

    private void emitGenericRules(IRElementConstraint element,
                                  String targetPath,
                                  Set<String> lines) {
        if (element.cardinality() != null) {
            lines.add("* " + targetPath + " " + element.cardinality().format());
        }
        if (element.fixedValue() != null) {
            IRFixedValueType type = element.fixedValueType() == null ? defaultFixedTypeForPath(targetPath) : element.fixedValueType();
            lines.add("* " + targetPath + " = " + formatFixedValue(element.fixedValue(), type));
        }
        if (element.bindings() != null) {
            for (IRBinding binding : element.bindings()) {
                if (binding.valueSetRef() != null && !binding.valueSetRef().isBlank()) {
                    lines.add("* " + targetPath + " from " + binding.valueSetRef() + " (" + bindingStrength(binding.strength()) + ")");
                }
            }
        }
        if (element.shortDescription() != null && !element.shortDescription().isBlank()) {
            lines.add("* " + targetPath + " ^short = \"" + FshUtil.escape(element.shortDescription()) + "\"");
        }
    }

    private void seedGlobalConstants(SemanticMappingModel model,
                                     Set<String> lines,
                                     Set<SemanticRule> usedRules) {
        for (SemanticRule rule : model.allRules()) {
            if (rule.conditional()) {
                continue;
            }
            boolean hasSource = rule.sources().stream().anyMatch(source -> source.path() != null && !source.path().isBlank());
            if (hasSource) {
                continue;
            }
            for (TargetNode target : rule.targets()) {
                String path = normalizeTargetPath(target.path());
                if (path == null || path.isBlank() || path.startsWith("@") || "meta.profile".equals(path)) {
                    continue;
                }
                if (target.constantValue() != null) {
                    usedRules.add(rule);
                    lines.add("* " + path + " = " + formatFixedValue(target.constantValue(), defaultFixedTypeForPath(path)));
                }
            }
        }
    }

    private String resolveParent(SemanticMappingModel model) {
        for (SemanticRule rule : model.allRules()) {
            for (TargetNode target : rule.targets()) {
                if ("meta.profile".equals(normalizeTargetPath(target.path())) && target.constantValue() != null) {
                    return target.constantValue();
                }
            }
        }
        return DEFAULT_PARENT_PREFIX + resourceName(null, model, "Resource");
    }

    private void markParentRuleUsed(SemanticMappingModel model,
                                    Set<SemanticRule> usedRules,
                                    String parent) {
        for (SemanticRule rule : model.allRules()) {
            for (TargetNode target : rule.targets()) {
                if ("meta.profile".equals(normalizeTargetPath(target.path())) && Objects.equals(parent, target.constantValue())) {
                    usedRules.add(rule);
                }
            }
        }
    }

    private String resourceName(String parent, SemanticMappingModel model, String fallback) {
        if (parent != null && !parent.isBlank()) {
            int slash = parent.lastIndexOf('/');
            String tail = slash >= 0 ? parent.substring(slash + 1) : parent;
            if (!tail.isBlank()) {
                return tail.replaceAll("[^A-Za-z0-9]", "");
            }
        }
        for (SemanticRule rule : model.allRules()) {
            for (TargetNode target : rule.targets()) {
                String createdType = target.createdType();
                if (createdType != null && !createdType.isBlank() && Character.isUpperCase(createdType.charAt(0))) {
                    return createdType.replaceAll("[^A-Za-z0-9]", "");
                }
            }
        }
        return fallback == null || fallback.isBlank() ? "Resource" : fallback.replaceAll("[^A-Za-z0-9]", "");
    }

    private String buildProfileName(String sourceProfileName, IRTemplate template, String resourceName) {
        String base = (sourceProfileName == null || sourceProfileName.isBlank())
                ? template.rootCdaType()
                : sourceProfileName;
        String sanitized = base.replaceAll("[^A-Za-z0-9]", "");
        String resource = resourceName == null || resourceName.isBlank() ? "Resource" : resourceName;
        return sanitized + "Fhir" + resource;
    }

    private String normalizeSourcePath(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        return path.replace("[x]", "").replace("..", ".");
    }

    private String normalizeTargetPath(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        String normalized = path.replace("[x]", "").replace("..", ".");
        while (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        int dot = normalized.indexOf('.');
        if (dot > 0 && Character.isUpperCase(normalized.charAt(0))) {
            return normalized.substring(dot + 1);
        }
        return normalized;
    }

    private String rootSegment(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        int dot = path.indexOf('.');
        return dot >= 0 ? path.substring(0, dot) : path;
    }

    private IRFixedValueType defaultFixedTypeForPath(String path) {
        if (path == null) {
            return IRFixedValueType.STRING;
        }
        if (path.endsWith(".code") || "status".equals(path)) {
            return IRFixedValueType.CODE;
        }
        return IRFixedValueType.STRING;
    }

    private String formatFixedValue(String fixedValue, IRFixedValueType type) {
        if (type == null || type == IRFixedValueType.STRING) {
            return "\"" + FshUtil.escape(fixedValue) + "\"";
        }
        if (type == IRFixedValueType.CODE) {
            return "#" + fixedValue;
        }
        if (type == IRFixedValueType.BOOLEAN) {
            if ("1".equals(fixedValue)) {
                return "true";
            }
            if ("0".equals(fixedValue)) {
                return "false";
            }
            return fixedValue.toLowerCase(Locale.ROOT);
        }
        return "\"" + FshUtil.escape(fixedValue) + "\"";
    }

    private String bindingStrength(IRBindingStrength strength) {
        return (strength == null ? IRBindingStrength.REQUIRED : strength).name().toLowerCase(Locale.ROOT);
    }
}
