package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.cda.CdaBindingStrength;
import net.ihe.gazelle.axiomcda.api.cda.CdaElementDefinition;
import net.ihe.gazelle.axiomcda.api.cda.CdaStructureDefinition;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.fsh.FshBundle;
import net.ihe.gazelle.axiomcda.api.ir.*;
import net.ihe.gazelle.axiomcda.api.port.CdaModelRepository;
import net.ihe.gazelle.axiomcda.api.port.IrToFshGenerator;
import net.ihe.gazelle.axiomcda.engine.util.FshUtil;
import net.ihe.gazelle.axiomcda.engine.util.NameUtil;
import net.ihe.gazelle.axiomcda.engine.util.ProfileNamingUtil;

import java.util.*;

public class DefaultIrToFshGenerator implements IrToFshGenerator {
    public static final String RESOURCES_DIR = "Resources";
    private static final String INVARIANTS_DIR = "Invariants";

    @Override
    public FshBundle generate(List<IRTemplate> templates, GenerationConfig config, CdaModelRepository cdaRepository) {
        Map<String, String> files = new LinkedHashMap<>();
        Map<String, String> profileIdByTemplateId = ProfileNamingUtil.resolveProfileIds(templates, config);
        Map<String, String> profileNameByTemplateId = ProfileNamingUtil.resolveProfileNames(templates, config);
        Map<String, String> rootTypeByTemplateId = new HashMap<>();

        for (IRTemplate template : templates) {
            rootTypeByTemplateId.put(template.id(), template.rootCdaType());
        }

        Map<String, IRInvariant> invariantsByName = new LinkedHashMap<>();

        for (IRTemplate template : templates) {
            String profileName = profileNameByTemplateId.get(template.id());
            String profileId = profileIdByTemplateId.get(template.id());
            Optional<CdaStructureDefinition> cdaDefinition = cdaRepository.findByName(template.rootCdaType());
            if (cdaDefinition.isEmpty()) {
                continue;
            }
            CdaStructureDefinition baseDefinition = cdaDefinition.get();

            StringBuilder builder = new StringBuilder();
            builder.append("Profile: ").append(profileName).append("\n");
            builder.append("Parent: ").append(baseDefinition.url()).append("\n");
            builder.append("Id: ").append(profileId).append("\n");
            builder.append("Title: \"").append(FshUtil.escape(ProfileNamingUtil.resolveTitle(template, config))).append("\"\n");
            builder.append("Description: \"").append(FshUtil.escape(resolveDescription(template))).append("\"\n");
            builder.append("* ^status = #draft\n");

            Map<String, List<IRTemplateInclude>> includeOnlyByPath = new HashMap<>();
            for (IRTemplateInclude include : template.includes()) {
                includeOnlyByPath.computeIfAbsent(include.path(), key -> new ArrayList<>()).add(include);
            }

            Map<String, IRElementConstraint> byPath = new TreeMap<>();
            for (IRElementConstraint constraint : template.elements()) {
                byPath.put(constraint.path(), constraint);
            }

            Set<String> allPaths = new TreeSet<>();
            allPaths.addAll(byPath.keySet());
            allPaths.addAll(includeOnlyByPath.keySet());
            for (String path : allPaths) {
                IRElementConstraint constraint = byPath.get(path);
                CdaElementDefinition baseElement = resolveBaseElement(baseDefinition, path, cdaRepository);
                if (constraint != null) {
                    if (constraint.cardinality() != null) {
                        IRCardinality effectiveCardinality = clampCardinality(constraint.cardinality(), baseElement);
                        builder.append("* ").append(path).append(" ").append(effectiveCardinality.format()).append("\n");
                    }
                    if (constraint.fixedValue() != null) {
                        if (baseElement == null || !baseElement.hasFixedValue()) {
                            IRFixedValueType fixedType = constraint.fixedValueType();
                            if (fixedType == null) {
                                fixedType = IRFixedValueType.STRING;
                            }
                            if (fixedType == IRFixedValueType.STRING && isBooleanElement(baseElement)) {
                                fixedType = IRFixedValueType.BOOLEAN;
                            }
                            builder.append("* ").append(path).append(" = ");
                            builder.append(formatFixedValue(constraint.fixedValue(), fixedType));
                            builder.append("\n");
                        }
                    }
                    for (IRBinding binding : constraint.bindings()) {
                        IRBindingStrength strength = effectiveBindingStrength(binding.strength(), baseElement);
                        builder.append("* ").append(path).append(" from ")
                                .append(binding.valueSetRef()).append(" (")
                                .append(bindingStrengthLabel(strength)).append(")\n");
                    }
                    if (constraint.shortDescription() != null) {
                        builder.append("* ").append(path).append(" ^short = \"")
                                .append(FshUtil.escape(constraint.shortDescription())).append("\"\n");
                    }
                }
                List<IRTemplateInclude> onlyTargets = includeOnlyByPath.get(path);
                if (onlyTargets != null) {
                    for (IRTemplateInclude include : onlyTargets) {
                        String targetName = profileNameByTemplateId.get(include.templateId());
                        if (targetName == null) {
                            continue;
                        }
                        String targetRoot = rootTypeByTemplateId.get(include.templateId());
                        if (!isCompatibleOnlyTarget(targetRoot, baseElement, cdaRepository)) {
                            continue;
                        }
                        builder.append("* ").append(path).append(" only ").append(targetName).append("\n");
                    }
                }
            }

            if (config.emitInvariants() && !template.invariants().isEmpty()) {
                for (IRInvariant invariant : template.invariants()) {
                    invariantsByName.putIfAbsent(invariant.name(), invariant);
                }
                for (IRInvariant invariant : template.invariants()) {
                    builder.append("* obeys ").append(invariant.name()).append("\n");
                }
            }

            String fileName = RESOURCES_DIR + "/" + profileName + ".fsh";
            files.put(fileName, builder.toString());
        }

        if (config.emitInvariants()) {
            for (IRInvariant invariant : invariantsByName.values()) {
                StringBuilder builder = new StringBuilder();
                builder.append("Invariant: ").append(invariant.name()).append("\n");
                if (invariant.description() != null) {
                    builder.append("Description: \"").append(FshUtil.escape(invariant.description())).append("\"\n");
                }
                builder.append("Severity: #").append(invariantSeverityLabel(invariant.severity())).append("\n");
                builder.append("Expression: \"").append(FshUtil.escape(invariant.expression())).append("\"\n");
                String fileName = INVARIANTS_DIR + "/" + invariant.name() + ".fsh";
                files.put(fileName, builder.toString());
            }
        }

        return new FshBundle(files);
    }

    private String resolveDescription(IRTemplate template) {
        if (template.description() != null && !template.description().isBlank()) {
            return template.description();
        }
        if (template.displayName() != null && !template.displayName().isBlank()) {
            return template.displayName();
        }
        return template.name() != null ? template.name() : "";
    }

    private String formatFixedValue(String value, IRFixedValueType type) {
        if (type == IRFixedValueType.BOOLEAN) {
            String trimmed = value == null ? "" : value.trim();
            if ("1".equals(trimmed)) {
                return "true";
            }
            if ("0".equals(trimmed)) {
                return "false";
            }
            if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
                return trimmed.toLowerCase(Locale.ROOT);
            }
            return trimmed;
        }
        if (type == IRFixedValueType.CODE) {
            return "#" + value;
        }
        return "\"" + FshUtil.escape(value) + "\"";
    }

    private IRCardinality clampCardinality(IRCardinality requested, CdaElementDefinition baseElement) {
        if (requested == null || baseElement == null) {
            return requested;
        }
        int min = requested.min();
        Integer baseMin = baseElement.min();
        if (baseMin != null) {
            min = Math.max(min, baseMin);
        }
        Integer baseMax = parseNumericMax(baseElement.max());
        if (baseMax != null) {
            min = Math.min(min, baseMax);
        }
        String max = requested.max();
        Integer requestedMax = parseNumericMax(max);
        if (baseMax != null) {
            int effectiveMax = requestedMax == null ? baseMax : Math.min(requestedMax, baseMax);
            if (effectiveMax < min) {
                effectiveMax = min;
            }
            max = String.valueOf(effectiveMax);
        } else if (requestedMax != null && requestedMax < min) {
            max = String.valueOf(min);
        }
        if (min == requested.min() && max.equals(requested.max())) {
            return requested;
        }
        return new IRCardinality(min, max);
    }

    private Integer parseNumericMax(String max) {
        if (max == null || max.isBlank() || "*".equals(max)) {
            return null;
        }
        try {
            return Integer.parseInt(max);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isBooleanElement(CdaElementDefinition baseElement) {
        if (baseElement == null || baseElement.typeCodes() == null) {
            return false;
        }
        for (String code : baseElement.typeCodes()) {
            if (code == null) {
                continue;
            }
            String lower = code.toLowerCase(Locale.ROOT);
            if (lower.equals("boolean") || lower.endsWith("/boolean") || lower.endsWith("/bl") || lower.endsWith("/bl-simple")) {
                return true;
            }
        }
        return false;
    }

    private boolean isCompatibleOnlyTarget(String targetRootType,
                                           CdaElementDefinition baseElement,
                                           CdaModelRepository cdaRepository) {
        if (targetRootType == null || targetRootType.isBlank()) {
            return false;
        }
        if (baseElement == null || baseElement.typeCodes() == null || baseElement.typeCodes().isEmpty()) {
            return true;
        }
        List<String> allowedTypes = resolveAllowedTypes(baseElement, cdaRepository);
        if (allowedTypes.isEmpty()) {
            return true;
        }
        String normalizedTarget = resolveKnownType(targetRootType, cdaRepository);
        String candidate = normalizedTarget != null ? normalizedTarget : targetRootType;
        for (String allowed : allowedTypes) {
            if (allowed.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private List<String> resolveAllowedTypes(CdaElementDefinition baseElement, CdaModelRepository cdaRepository) {
        if (baseElement == null || baseElement.typeCodes() == null) {
            return List.of();
        }
        List<String> resolved = new ArrayList<>();
        for (String code : baseElement.typeCodes()) {
            if (code == null || code.isBlank()) {
                continue;
            }
            String normalized = code;
            int slash = code.lastIndexOf('/');
            if (slash >= 0 && slash + 1 < code.length()) {
                normalized = code.substring(slash + 1);
            }
            String known = resolveKnownType(normalized, cdaRepository);
            resolved.add(known != null ? known : normalized);
        }
        return resolved;
    }

    private CdaElementDefinition resolveBaseElement(CdaStructureDefinition baseDefinition,
                                                    String path,
                                                    CdaModelRepository cdaRepository) {
        if (baseDefinition == null || path == null || path.isBlank() || cdaRepository == null) {
            return null;
        }
        CdaElementDefinition direct = baseDefinition.elementsByPath().get(baseDefinition.name() + "." + path);
        if (direct != null) {
            return direct;
        }
        String[] segments = path.split("\\.");
        String currentType = baseDefinition.name();
        CdaElementDefinition currentElement = null;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if ("item".equals(segment) && i + 1 < segments.length) {
                String combined = "item." + segments[i + 1];
                CdaElementDefinition combinedElement = findElement(currentType, combined, cdaRepository);
                if (combinedElement != null) {
                    currentElement = combinedElement;
                    i++;
                } else {
                    currentElement = findElement(currentType, segment, cdaRepository);
                }
            } else {
                currentElement = findElement(currentType, segment, cdaRepository);
            }
            if (currentElement == null) {
                return null;
            }
            if (i == segments.length - 1) {
                return currentElement;
            }
            String nextType = resolveTypeName(currentElement.typeCodes(), cdaRepository);
            if (nextType == null) {
                return null;
            }
            currentType = nextType;
        }
        return currentElement;
    }

    private CdaElementDefinition findElement(String currentType,
                                             String segment,
                                             CdaModelRepository cdaRepository) {
        return cdaRepository.findByName(currentType)
                .map(definition -> definition.elementsByPath().get(definition.name() + "." + segment))
                .orElse(null);
    }

    private String resolveTypeName(List<String> typeCodes, CdaModelRepository cdaRepository) {
        if (typeCodes == null || typeCodes.isEmpty()) {
            return null;
        }
        for (String code : typeCodes) {
            if (code == null || code.isBlank()) {
                continue;
            }
            String normalized = code;
            int slash = code.lastIndexOf('/');
            if (slash >= 0 && slash + 1 < code.length()) {
                normalized = code.substring(slash + 1);
            }
            String resolved = resolveKnownType(normalized, cdaRepository);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    private String resolveKnownType(String candidate, CdaModelRepository cdaRepository) {
        if (cdaRepository.findByName(candidate).isPresent()) {
            return candidate;
        }
        if (candidate.contains("-")) {
            String underscored = candidate.replace('-', '_');
            if (cdaRepository.findByName(underscored).isPresent()) {
                return underscored;
            }
        }
        if (candidate.contains("_")) {
            String dashed = candidate.replace('_', '-');
            if (cdaRepository.findByName(dashed).isPresent()) {
                return dashed;
            }
        }
        return null;
    }

    private IRBindingStrength effectiveBindingStrength(IRBindingStrength requested, CdaElementDefinition baseElement) {
        if (baseElement == null || baseElement.bindingStrength() == null) {
            return requested;
        }
        IRBindingStrength baseStrength = mapBindingStrength(baseElement.bindingStrength());
        if (baseStrength == null) {
            return requested;
        }
        return strongerBinding(baseStrength, requested);
    }

    private IRBindingStrength strongerBinding(IRBindingStrength left, IRBindingStrength right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return bindingRank(left) >= bindingRank(right) ? left : right;
    }

    private int bindingRank(IRBindingStrength strength) {
        return switch (strength) {
            case REQUIRED -> 3;
            case EXTENSIBLE -> 2;
            case PREFERRED -> 1;
        };
    }

    private IRBindingStrength mapBindingStrength(CdaBindingStrength strength) {
        if (strength == null) {
            return null;
        }
        return switch (strength) {
            case REQUIRED -> IRBindingStrength.REQUIRED;
            case EXTENSIBLE -> IRBindingStrength.EXTENSIBLE;
            case PREFERRED -> IRBindingStrength.PREFERRED;
        };
    }

    private String bindingStrengthLabel(IRBindingStrength strength) {
        return switch (strength) {
            case REQUIRED -> "required";
            case EXTENSIBLE -> "extensible";
            case PREFERRED -> "preferred";
        };
    }

    private String invariantSeverityLabel(IRInvariantSeverity severity) {
        return switch (severity) {
            case ERROR -> "error";
            case WARNING -> "warning";
        };
    }
}
