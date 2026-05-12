package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.ir.IRBinding;
import net.ihe.gazelle.axiomcda.api.ir.IRBindingStrength;
import net.ihe.gazelle.axiomcda.api.ir.IRCardinality;
import net.ihe.gazelle.axiomcda.api.ir.IRFixedValueType;
import net.ihe.gazelle.axiomcda.api.ir.IRElementConstraint;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.engine.util.FshUtil;
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

abstract class AbstractStructureMapIrToFhirFshGenerator implements IrToFhirFshGenerator {

    private static final String DEFAULT_PARENT_PREFIX = "http://hl7.org/fhir/StructureDefinition/";

    @Override
    public ObservationFhirConversionResult generate(IRTemplate template,
                                                    String sourceProfileName,
                                                    SemanticMappingModel semanticModel) throws IOException {
        validateTemplate(template);
        if (semanticModel == null || semanticModel.allRules().isEmpty()) {
            throw new IllegalArgumentException("A StructureMap with analyzable mapping rules is required");
        }

        String parent = resolveParent(semanticModel);
        String resourceName = resourceName(parent, semanticModel, template.rootCdaType());
        String profileName = FhirProfileSupport.buildProfileName(sourceProfileName, template, resourceName);
        String description = defaultDescription(template, resourceName);

        Map<String, List<SemanticRule>> rulesBySourcePath = indexRulesBySourcePath(semanticModel);
        Set<SemanticRule> usedRules = Collections.newSetFromMap(new IdentityHashMap<>());
        List<String> diagnostics = new ArrayList<>();
        LinkedHashSet<String> lines = FhirProfileSupport.initializeProfileLines(profileName, parent, description);

        seedGlobalConstants(semanticModel, lines, usedRules);
        markParentRuleUsed(semanticModel, usedRules, parent);

        for (IRElementConstraint element : template.elements()) {
            Resolution resolution = resolveElementTarget(element, rulesBySourcePath);
            if (resolution.targetPath() == null) {
                continue;
            }
            resolution.usedRules().forEach(usedRules::add);
            emitRules(element, resolution.targetPath(), lines);
        }

        return new ObservationFhirConversionResult(
                profileName,
                String.join("\n", lines) + "\n",
                diagnostics,
                SemanticMappingModelFilter.filterByIdentity(semanticModel, usedRules)
        );
    }

    protected void validateTemplate(IRTemplate template) {
        if (template == null) {
            throw new IllegalArgumentException("template must be set");
        }
    }

    protected String defaultDescription(IRTemplate template, String resourceName) {
        return template.description() == null || template.description().isBlank()
                ? "FHIR " + resourceName + " profile generated from CDA IR and uploaded StructureMap."
                : template.description();
    }

    protected String resolveAdditionalTargetPath(IRElementConstraint element,
                                                 Map<String, List<SemanticRule>> rulesBySourcePath) {
        return null;
    }

    protected List<SemanticRule> additionalUsedRules(IRElementConstraint element,
                                                     Map<String, List<SemanticRule>> rulesBySourcePath,
                                                     String resolvedTargetPath) {
        return List.of();
    }

    protected IRCardinality normalizeCardinality(String targetPath, IRCardinality cardinality) {
        return cardinality;
    }

    protected Map<String, List<SemanticRule>> indexRulesBySourcePath(SemanticMappingModel semanticModel) {
        Map<String, List<SemanticRule>> rulesBySourcePath = new LinkedHashMap<>();
        for (SemanticRule rule : semanticModel.allRules()) {
            addRuleForSourcePath(rulesBySourcePath, normalizeSourcePath(rule.primarySourcePath()), rule);
            for (SourceNode source : rule.sources()) {
                addRuleForSourcePath(rulesBySourcePath, normalizeSourcePath(source.path()), rule);
            }
        }
        return rulesBySourcePath;
    }

    protected void addRuleForSourcePath(Map<String, List<SemanticRule>> rulesBySourcePath,
                                        String sourcePath,
                                        SemanticRule rule) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return;
        }
        rulesBySourcePath.computeIfAbsent(sourcePath, ignored -> new ArrayList<>()).add(rule);
    }

    protected Resolution resolveElementTarget(IRElementConstraint element,
                                              Map<String, List<SemanticRule>> rulesBySourcePath) {
        List<SemanticRule> matchingRules = directRulesForElement(element, rulesBySourcePath);
        String targetPath = resolveTargetPath(element, matchingRules);
        List<SemanticRule> usedRules = new ArrayList<>(matchingRules);
        if (targetPath == null) {
            targetPath = resolveAdditionalTargetPath(element, rulesBySourcePath);
            if (targetPath != null) {
                usedRules.addAll(additionalUsedRules(element, rulesBySourcePath, targetPath));
            }
        }
        if (targetPath == null) {
            return new Resolution(null, List.of());
        }
        return new Resolution(targetPath, usedRules);
    }

    protected List<SemanticRule> directRulesForElement(IRElementConstraint element,
                                                       Map<String, List<SemanticRule>> rulesBySourcePath) {
        List<SemanticRule> matchingRules = rulesBySourcePath.getOrDefault(element.path(), List.of());
        if (matchingRules.isEmpty() && !element.path().contains(".")) {
            matchingRules = rulesBySourcePath.getOrDefault(rootSegment(element.path()), List.of());
        }
        return matchingRules;
    }

    protected String resolveTargetPath(IRElementConstraint element, List<SemanticRule> rules) {
        String shortest = resolveTargetPathForRules(rules);
        if (shortest == null) {
            return null;
        }
        if (!element.path().contains(".")) {
            return rootSegment(shortest);
        }
        return shortest;
    }

    protected void emitRules(IRElementConstraint element,
                             String targetPath,
                             Set<String> lines) {
        if (element.cardinality() != null) {
            lines.add("* " + targetPath + " " + normalizeCardinality(targetPath, element.cardinality()).format());
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

    protected void seedGlobalConstants(SemanticMappingModel model,
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

    protected String resolveParent(SemanticMappingModel model) {
        for (SemanticRule rule : model.allRules()) {
            for (TargetNode target : rule.targets()) {
                if ("meta.profile".equals(normalizeTargetPath(target.path())) && target.constantValue() != null) {
                    return target.constantValue();
                }
            }
        }
        String targetType = SemanticMappingModelSupport.resolvePrimaryTargetType(model);
        if (targetType != null && !targetType.isBlank()) {
            return DEFAULT_PARENT_PREFIX + targetType;
        }
        return DEFAULT_PARENT_PREFIX + resourceName(null, model, "Resource");
    }

    protected void markParentRuleUsed(SemanticMappingModel model,
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

    protected String resourceName(String parent, SemanticMappingModel model, String fallback) {
        if (parent != null && !parent.isBlank()) {
            int slash = parent.lastIndexOf('/');
            String tail = slash >= 0 ? parent.substring(slash + 1) : parent;
            if (!tail.isBlank()) {
                return tail.replaceAll("[^A-Za-z0-9]", "");
            }
        }
        String targetType = SemanticMappingModelSupport.resolvePrimaryTargetType(model);
        if (targetType != null && !targetType.isBlank()) {
            return targetType.replaceAll("[^A-Za-z0-9]", "");
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

    protected boolean hasMappedDescendants(String sourcePath, Map<String, List<SemanticRule>> rulesBySourcePath) {
        if (sourcePath == null || sourcePath.isBlank() || rulesBySourcePath == null || rulesBySourcePath.isEmpty()) {
            return false;
        }
        String prefix = sourcePath + ".";
        for (Map.Entry<String, List<SemanticRule>> entry : rulesBySourcePath.entrySet()) {
            String candidatePath = entry.getKey();
            if (candidatePath == null || !candidatePath.startsWith(prefix)) {
                continue;
            }
            if (resolveTargetPathForRules(entry.getValue()) != null) {
                return true;
            }
        }
        return false;
    }

    protected String resolveTargetPathForRules(List<SemanticRule> rules) {
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
        return candidates.stream()
                .min((left, right) -> Integer.compare(left.length(), right.length()))
                .orElse(null);
    }

    protected String normalizeSourcePath(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        return path.replace("[x]", "").replace("..", ".");
    }

    protected String normalizeTargetPath(String path) {
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

    protected String rootSegment(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        int dot = path.indexOf('.');
        return dot >= 0 ? path.substring(0, dot) : path;
    }

    protected IRFixedValueType defaultFixedTypeForPath(String path) {
        if (path == null) {
            return IRFixedValueType.STRING;
        }
        if (path.endsWith(".code") || "status".equals(path)) {
            return IRFixedValueType.CODE;
        }
        return IRFixedValueType.STRING;
    }

    protected String formatFixedValue(String fixedValue, IRFixedValueType type) {
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

    protected String bindingStrength(IRBindingStrength strength) {
        return (strength == null ? IRBindingStrength.REQUIRED : strength).name().toLowerCase(Locale.ROOT);
    }

    protected record Resolution(String targetPath, List<SemanticRule> usedRules) {
        protected Resolution {
            usedRules = usedRules == null ? List.of() : List.copyOf(usedRules);
        }
    }
}
