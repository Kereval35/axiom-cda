package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.bbr.*;
import net.ihe.gazelle.axiomcda.api.cda.CdaElementDefinition;
import net.ihe.gazelle.axiomcda.api.cda.CdaStructureDefinition;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.config.TemplateSelection;
import net.ihe.gazelle.axiomcda.api.ir.*;
import net.ihe.gazelle.axiomcda.api.port.BbrToIrTransformer;
import net.ihe.gazelle.axiomcda.api.port.CdaModelRepository;
import net.ihe.gazelle.axiomcda.engine.util.NameUtil;
import net.ihe.gazelle.axiomcda.engine.util.TextUtil;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultBbrToIrTransformer implements BbrToIrTransformer {
    private static final Pattern COUNT_ASSERT_PATTERN = Pattern.compile("count\\(([^)]+)\\)\\s*(=|>=|<=|>)\\s*(\\d+)");
    private static final Map<String, String> ROOT_TYPE_ALIASES = Map.of(
            "assignedPerson", "Person",
            "assignedAuthoringDevice", "AuthoringDevice",
            "representedOrganization", "Organization",
            "representedCustodianOrganization", "CustodianOrganization",
            "addr", "AD",
            "telecom", "TEL",
            "name", "PN",
            "id", "II",
            "participant", "Participant1",
            "performer", "Performer1"
    );

    @Override
    public IrTransformResult transform(Decor decor, GenerationConfig config, CdaModelRepository cdaRepository) {
        if (decor == null) {
            throw new IllegalArgumentException("decor must be set");
        }
        List<IRDiagnostic> diagnostics = new ArrayList<>();
        List<IRTemplate> templates = new ArrayList<>();

        Rules rules = decor.getRules();
        if (rules == null) {
            return new IrTransformResult(List.of(), List.of());
        }

        Map<String, List<TemplateDefinition>> byId = new HashMap<>();
        for (Object entry : rules.getTemplateAssociationOrTemplate()) {
            if (entry instanceof TemplateDefinition template) {
                if (!isActive(template)) {
                    continue;
                }
                if (template.getId() == null || template.getId().isBlank()) {
                    continue;
                }
                byId.computeIfAbsent(template.getId(), key -> new ArrayList<>()).add(template);
            }
        }

        Map<String, TemplateDefinition> latestById = new HashMap<>();
        for (Map.Entry<String, List<TemplateDefinition>> entry : byId.entrySet()) {
            TemplateDefinition selected = entry.getValue().stream()
                    .max(Comparator.comparing(this::effectiveInstant))
                    .orElse(null);
            if (selected != null) {
                latestById.put(entry.getKey(), selected);
            }
        }

        String preferredLanguage = decor.getProject() != null ? decor.getProject().getDefaultLanguage() : null;

        Set<String> selectedIds = selectTemplateIds(latestById, config.templateSelection());
        Set<String> expandedIds = expandIncludes(selectedIds, latestById);

        for (String templateId : expandedIds) {
            TemplateDefinition template = latestById.get(templateId);
            if (template == null) {
                continue;
            }
            TemplateBuildContext context = new TemplateBuildContext(template, preferredLanguage, config, cdaRepository, latestById, diagnostics);
            IRTemplate irTemplate = buildTemplate(context, config);
            if (irTemplate != null) {
                templates.add(irTemplate);
            }
        }

        templates.sort(Comparator.comparing(IRTemplate::rootCdaType).thenComparing(IRTemplate::id));
        return new IrTransformResult(templates, diagnostics);
    }

    private IRTemplate buildTemplate(TemplateBuildContext context, GenerationConfig config) {
        TemplateDefinition template = context.template;
        RuleDefinition rootRule = findRootRule(template);
        if (rootRule == null || rootRule.getName() == null) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, null, "No root element found");
            return null;
        }

        ParsedName rootName = parseElementName(rootRule.getName());
        if (rootName == null || rootName.baseName == null || rootName.baseName.isBlank()) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, null, "Invalid root element name");
            return null;
        }

        if (rootName.predicate != null) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, rootName.baseName, "Root element predicate ignored");
        }

        String rootCdaType = resolveRootCdaType(rootName.baseName);
        Optional<CdaStructureDefinition> cdaDefinition = context.cdaRepository.findByName(rootCdaType);
        if (cdaDefinition.isEmpty()) {
            context.addDiagnostic(IRDiagnosticSeverity.ERROR, rootName.baseName, "CDA structure definition not found for root type");
            return null;
        }
        context.setCdaDefinition(cdaDefinition.get());

        Map<String, ElementConstraintBuilder> builders = new LinkedHashMap<>();
        List<IRTemplateInclude> includes = new ArrayList<>();
        List<IRInvariant> invariants = new ArrayList<>();

        String templateDescription = TextUtil.selectDescription(template.getDesc(), context.preferredLanguage);

        processAttributes(rootRule, "", context, builders);
        processNested(rootRule, "", context, builders, includes, invariants, config);
        applyNullFlavorPolicy(builders, context, config);

        List<IRElementConstraint> elementConstraints = builders.values().stream()
                .map(ElementConstraintBuilder::build)
                .filter(Objects::nonNull)
                .toList();

        return new IRTemplate(
                template.getId(),
                template.getName(),
                template.getDisplayName(),
                templateDescription,
                rootCdaType,
                elementConstraints,
                includes,
                invariants
        );
    }

    private void processNested(RuleDefinition rule,
                               String parentPath,
                               TemplateBuildContext context,
                               Map<String, ElementConstraintBuilder> builders,
                               List<IRTemplateInclude> includes,
                               List<IRInvariant> invariants,
                               GenerationConfig config) {
        if (rule.getLetOrAssertOrReport() == null) {
            return;
        }
        for (Object entry : rule.getLetOrAssertOrReport()) {
            if (entry instanceof RuleDefinition childRule) {
                processRule(childRule, parentPath, context, builders, includes, invariants, config);
            } else if (entry instanceof IncludeDefinition include) {
                processInclude(include, parentPath, context, builders, includes);
            } else if (entry instanceof Assert assertion) {
                if (config.emitInvariants()) {
                    Optional<IRInvariant> invariant = parseInvariant(assertion, context, invariants.size() + 1);
                    invariant.ifPresent(invariants::add);
                }
            } else if (entry instanceof ChoiceDefinition) {
                context.addDiagnostic(IRDiagnosticSeverity.WARNING, parentPath, "Choice elements are not supported in v0");
            }
        }
    }

    private void processRule(RuleDefinition rule,
                             String parentPath,
                             TemplateBuildContext context,
                             Map<String, ElementConstraintBuilder> builders,
                             List<IRTemplateInclude> includes,
                             List<IRInvariant> invariants,
                             GenerationConfig config) {
        if (rule.getName() == null) {
            return;
        }
        ParsedName parsed = parseElementName(rule.getName());
        if (parsed == null || parsed.baseName == null || parsed.baseName.isBlank()) {
            return;
        }
        if (parsed.predicate != null) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, parsed.baseName,
                    "Predicated element not supported (slicing skipped)");
            return;
        }
        String currentPath = parentPath.isEmpty() ? parsed.baseName : parentPath + "." + parsed.baseName;
        String normalizedPath = context.normalizePath(currentPath);
        if (normalizedPath == null) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, currentPath, "Unmapped CDA path");
            return;
        }
        ElementConstraintBuilder builder = builders.computeIfAbsent(normalizedPath, ElementConstraintBuilder::new);
        builder.applyDatatype(rule.getDatatype());
        IRCardinality cardinality = cardinalityFromRule(rule);
        if (cardinality != null) {
            builder.applyCardinality(cardinality, context);
        }
        String shortDesc = TextUtil.selectDescription(rule.getDesc(), context.preferredLanguage);
        if (shortDesc != null) {
            builder.applyShortDescription(shortDesc);
        }
        List<IRBinding> bindings = bindingsFromVocabulary(rule.getVocabulary(), context, rule.getStrength());
        if (!bindings.isEmpty()) {
            builder.applyBindings(bindings, context);
        }

        processAttributes(rule, normalizedPath, context, builders);
        processNested(rule, normalizedPath, context, builders, includes, invariants, config);
    }

    private void processAttributes(RuleDefinition rule,
                                   String parentPath,
                                   TemplateBuildContext context,
                                   Map<String, ElementConstraintBuilder> builders) {
        if (rule.getAttribute() == null) {
            return;
        }
        for (Attribute attribute : rule.getAttribute()) {
            if (attribute.getName() == null || attribute.getName().isBlank()) {
                continue;
            }
            String normalized = normalizeName(attribute.getName());
            String attrPath = parentPath.isEmpty() ? normalized : parentPath + "." + normalized;
            String normalizedPath = context.normalizePath(attrPath);
            if (normalizedPath == null) {
                context.addDiagnostic(IRDiagnosticSeverity.WARNING, attrPath, "Unmapped CDA attribute path");
                continue;
            }
            ElementConstraintBuilder builder = builders.computeIfAbsent(normalizedPath, ElementConstraintBuilder::new);
            builder.applyDatatype(attribute.getDatatype());
            IRCardinality cardinality = cardinalityFromAttribute(attribute);
            if (cardinality != null) {
                builder.applyCardinality(cardinality, context);
            }
            if (attribute.getValue() != null && !attribute.getValue().isBlank()) {
                builder.applyFixedValue(attribute.getValue(), attribute.getDatatype(), attribute.getName());
            }
            String shortDesc = TextUtil.selectDescription(attribute.getDesc(), context.preferredLanguage);
            if (shortDesc != null) {
                builder.applyShortDescription(shortDesc);
            }
            List<IRBinding> bindings = bindingsFromVocabulary(attribute.getVocabulary(), context, null);
            if (!bindings.isEmpty()) {
                builder.applyBindings(bindings, context);
            }
        }
    }

    private void processInclude(IncludeDefinition include,
                                String parentPath,
                                TemplateBuildContext context,
                                Map<String, ElementConstraintBuilder> builders,
                                List<IRTemplateInclude> includes) {
        String ref = include.getRef();
        if (ref == null || ref.isBlank()) {
            return;
        }
        TemplateDefinition includedTemplate = context.templateById.get(ref);
        if (includedTemplate == null) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, parentPath, "Included template not found: " + ref);
            return;
        }
        RuleDefinition includedRoot = findRootRule(includedTemplate);
        if (includedRoot == null || includedRoot.getName() == null) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, parentPath, "Included template missing root element: " + ref);
            return;
        }
        ParsedName includeName = parseElementName(includedRoot.getName());
        if (includeName == null || includeName.baseName == null || includeName.baseName.isBlank()) {
            return;
        }
        if (includeName.predicate != null) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, parentPath, "Included template root predicate ignored");
        }
        String includePath = parentPath.isEmpty() ? includeName.baseName : parentPath + "." + includeName.baseName;
        String normalizedPath = context.normalizePath(includePath);
        if (normalizedPath == null) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, includePath, "Unmapped include path");
            return;
        }
        IRCardinality cardinality = cardinalityFromInclude(include);
        includes.add(new IRTemplateInclude(normalizedPath, ref, cardinality));
        if (cardinality != null) {
            ElementConstraintBuilder builder = builders.computeIfAbsent(normalizedPath, ElementConstraintBuilder::new);
            builder.applyCardinality(cardinality, context);
        }
    }

    private Optional<IRInvariant> parseInvariant(Assert assertion, TemplateBuildContext context, int index) {
        String test = assertion.getTest();
        if (test == null) {
            return Optional.empty();
        }
        Matcher matcher = COUNT_ASSERT_PATTERN.matcher(test);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String rawPath = matcher.group(1);
        String operator = matcher.group(2);
        String count = matcher.group(3);
        String normalizedPath = normalizeInvariantPath(rawPath, context.rootCdaType());
        if (normalizedPath == null || normalizedPath.isBlank()) {
            return Optional.empty();
        }
        String mappedPath = context.normalizePath(normalizedPath);
        if (mappedPath != null) {
            normalizedPath = mappedPath;
        }
        String expression = normalizedPath + ".count() " + operator + " " + count;
        String description = TextUtil.flattenMixedContent(assertion.getContent());
        String prefix = context.config.naming().profilePrefix();
        String name = (prefix == null ? "" : prefix) + context.rootCdaType() + "Inv" + index;
        return Optional.of(new IRInvariant(name, description, IRInvariantSeverity.ERROR, expression));
    }

    private void applyNullFlavorPolicy(Map<String, ElementConstraintBuilder> builders,
                                       TemplateBuildContext context,
                                       GenerationConfig config) {
        if (config.nullFlavorPolicy() == null || config.nullFlavorPolicy().forbiddenPaths().isEmpty()) {
            return;
        }
        for (String path : config.nullFlavorPolicy().forbiddenPaths()) {
            if (path == null) {
                continue;
            }
            String trimmed = path.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String nullFlavorPath = trimmed.equals("/") ? "nullFlavor" : trimmed;
            if (!nullFlavorPath.endsWith("nullFlavor")) {
                nullFlavorPath = nullFlavorPath + ".nullFlavor";
            }
            String normalizedPath = context.normalizePath(nullFlavorPath);
            if (normalizedPath == null) {
                context.addDiagnostic(IRDiagnosticSeverity.WARNING, nullFlavorPath, "NullFlavor path not in CDA model");
                continue;
            }
            ElementConstraintBuilder builder = builders.computeIfAbsent(normalizedPath, ElementConstraintBuilder::new);
            builder.applyCardinality(new IRCardinality(0, "0"), context);
        }
    }

    private String normalizeInvariantPath(String rawPath, String rootType) {
        String path = rawPath.trim();
        path = path.replaceAll("^//", "");
        path = path.replaceAll("^\\/\\/", "");
        path = path.replaceAll("^\\*:\\/?", "");
        path = path.replaceAll("\\[.*?\\]", "");
        path = path.replace("hl7:", "");
        path = path.replace("*:", "");
        path = path.replace("/", ".");
        path = path.replace("@", "");
        path = path.replaceAll("^\\.", "");
        if (rootType != null && path.startsWith(rootType + ".")) {
            path = path.substring(rootType.length() + 1);
        }
        return path;
    }

    private boolean matchesSelection(TemplateDefinition template, TemplateSelection selection) {
        if (selection == null) {
            return true;
        }
        if (!selection.templateIds().isEmpty()) {
            return selection.templateIds().contains(template.getId());
        }
        if (selection.classificationTypes().isEmpty()) {
            return true;
        }
        for (TemplateProperties properties : template.getClassification()) {
            if (properties.getType() != null && selection.classificationTypes().contains(properties.getType().value())) {
                return true;
            }
        }
        return false;
    }

    private boolean isActive(TemplateDefinition template) {
        TemplateStatusCodeLifeCycle status = template.getStatusCode();
        if (status == null) {
            return true;
        }
        return status != TemplateStatusCodeLifeCycle.RETIRED
                && status != TemplateStatusCodeLifeCycle.CANCELLED
                && status != TemplateStatusCodeLifeCycle.REJECTED;
    }

    private RuleDefinition findRootRule(TemplateDefinition template) {
        if (template.getAttributeOrChoiceOrElement() == null) {
            return null;
        }
        for (Object entry : template.getAttributeOrChoiceOrElement()) {
            if (entry instanceof RuleDefinition rule && rule.getName() != null) {
                return rule;
            }
            if (entry instanceof ChoiceDefinition choice && choice.getIncludeOrElementOrConstraint() != null) {
                for (Object choiceEntry : choice.getIncludeOrElementOrConstraint()) {
                    if (choiceEntry instanceof RuleDefinition rule && rule.getName() != null) {
                        return rule;
                    }
                }
            }
        }
        return null;
    }

    private Instant effectiveInstant(TemplateDefinition template) {
        XMLGregorianCalendar date = template.getEffectiveDate();
        if (date == null) {
            return Instant.EPOCH;
        }
        return date.toGregorianCalendar().toInstant();
    }

    private IRCardinality cardinalityFromRule(RuleDefinition rule) {
        Integer min = rule.getMinimumMultiplicity();
        String max = rule.getMaximumMultiplicity();
        if (min == null && max == null && rule.getIsMandatory() != null) {
            min = rule.getIsMandatory() ? 1 : 0;
            max = "1";
        }
        if (min == null && max == null) {
            return null;
        }
        return new IRCardinality(min == null ? 0 : min, max == null ? "1" : max);
    }

    private IRCardinality cardinalityFromInclude(IncludeDefinition include) {
        Integer min = include.getMinimumMultiplicity();
        String max = include.getMaximumMultiplicity();
        if (min == null && max == null && include.getIsMandatory() != null) {
            min = include.getIsMandatory() ? 1 : 0;
            max = "1";
        }
        if (min == null && max == null) {
            return null;
        }
        return new IRCardinality(min == null ? 0 : min, max == null ? "1" : max);
    }

    private IRCardinality cardinalityFromAttribute(Attribute attribute) {
        if (Boolean.TRUE.equals(attribute.isProhibited())) {
            return new IRCardinality(0, "0");
        }
        if (attribute.isIsOptional() != null) {
            return new IRCardinality(attribute.isIsOptional() ? 0 : 1, "1");
        }
        return null;
    }

    private List<IRBinding> bindingsFromVocabulary(List<Vocabulary> vocabulary,
                                                   TemplateBuildContext context,
                                                   CodingStrengthType strength) {
        if (vocabulary == null || vocabulary.isEmpty()) {
            return List.of();
        }
        List<IRBinding> bindings = new ArrayList<>();
        for (Vocabulary vocab : vocabulary) {
            String valueSet = vocab.getValueSet();
            String canonical = null;
            if (valueSet != null) {
                canonical = context.resolveValueSet(valueSet);
                if (canonical == null) {
                    context.addDiagnostic(IRDiagnosticSeverity.WARNING, null, "Unresolved ValueSet: " + valueSet);
                }
            }
            IRBindingStrength bindingStrength = mapStrength(strength, context);
            if (canonical != null) {
                bindings.add(new IRBinding(bindingStrength, canonical, vocab.getCodeSystem()));
            }
        }
        return bindings;
    }

    private IRBindingStrength mapStrength(CodingStrengthType strength, TemplateBuildContext context) {
        if (strength == null) {
            return context.defaultBindingStrength();
        }
        return switch (strength) {
            case required -> IRBindingStrength.REQUIRED;
            case extensible -> IRBindingStrength.EXTENSIBLE;
            case preferred, example -> IRBindingStrength.PREFERRED;
        };
    }

    private ParsedName parseElementName(String rawName) {
        if (rawName == null) {
            return null;
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String namePart = trimmed;
        int bracket = trimmed.indexOf('[');
        String predicate = null;
        if (bracket >= 0) {
            int end = trimmed.lastIndexOf(']');
            predicate = end > bracket ? trimmed.substring(bracket + 1, end) : trimmed.substring(bracket + 1);
            namePart = trimmed.substring(0, bracket);
        }
        int colon = namePart.lastIndexOf(':');
        String prefix = colon >= 0 ? namePart.substring(0, colon) : null;
        String localName = colon >= 0 ? namePart.substring(colon + 1) : namePart;
        if (localName.startsWith("@")) {
            localName = localName.substring(1);
        }
        String baseName = localName;
        if ("sdtc".equalsIgnoreCase(prefix)) {
            baseName = "sdtc" + NameUtil.upperFirst(localName);
        }
        return new ParsedName(baseName, predicate);
    }

    private String normalizeName(String rawName) {
        ParsedName parsed = parseElementName(rawName);
        return parsed == null ? rawName : parsed.baseName;
    }

    private String resolveRootCdaType(String baseName) {
        if (baseName == null) {
            return null;
        }
        String alias = ROOT_TYPE_ALIASES.get(baseName);
        if (alias != null) {
            return alias;
        }
        return NameUtil.upperFirst(baseName);
    }

    private Set<String> selectTemplateIds(Map<String, TemplateDefinition> latestById, TemplateSelection selection) {
        Set<String> selected = new LinkedHashSet<>();
        for (TemplateDefinition template : latestById.values()) {
            if (matchesSelection(template, selection)) {
                selected.add(template.getId());
            }
        }
        if (selection != null && !selection.templateIds().isEmpty()) {
            selected.addAll(selection.templateIds());
        }
        return selected;
    }

    private Set<String> expandIncludes(Set<String> seedIds, Map<String, TemplateDefinition> latestById) {
        Set<String> expanded = new LinkedHashSet<>(seedIds);
        Deque<String> queue = new ArrayDeque<>(seedIds);
        while (!queue.isEmpty()) {
            String templateId = queue.removeFirst();
            TemplateDefinition template = latestById.get(templateId);
            if (template == null) {
                continue;
            }
            for (String includeId : collectIncludes(template)) {
                if (expanded.add(includeId)) {
                    queue.add(includeId);
                }
            }
        }
        return expanded;
    }

    private List<String> collectIncludes(TemplateDefinition template) {
        List<String> includes = new ArrayList<>();
        if (template.getAttributeOrChoiceOrElement() == null) {
            return includes;
        }
        for (Object entry : template.getAttributeOrChoiceOrElement()) {
            if (entry instanceof IncludeDefinition include) {
                if (include.getRef() != null && !include.getRef().isBlank()) {
                    includes.add(include.getRef());
                }
            } else if (entry instanceof RuleDefinition rule) {
                collectIncludes(rule, includes);
            }
        }
        return includes;
    }

    private void collectIncludes(RuleDefinition rule, List<String> includes) {
        if (rule.getLetOrAssertOrReport() == null) {
            return;
        }
        for (Object entry : rule.getLetOrAssertOrReport()) {
            if (entry instanceof IncludeDefinition include) {
                if (include.getRef() != null && !include.getRef().isBlank()) {
                    includes.add(include.getRef());
                }
            } else if (entry instanceof RuleDefinition childRule) {
                collectIncludes(childRule, includes);
            }
        }
    }

    private static final class ParsedName {
        private final String baseName;
        private final String predicate;

        private ParsedName(String baseName, String predicate) {
            this.baseName = baseName;
            this.predicate = predicate;
        }
    }

    private static final class SegmentResolution {
        private final String segment;
        private final String nextType;

        private SegmentResolution(String segment, String nextType) {
            this.segment = segment;
            this.nextType = nextType;
        }
    }

    private static final class TemplateBuildContext {
        private final TemplateDefinition template;
        private final String preferredLanguage;
        private final GenerationConfig config;
        private final CdaModelRepository cdaRepository;
        private final Map<String, TemplateDefinition> templateById;
        private final List<IRDiagnostic> diagnostics;
        private CdaStructureDefinition cdaDefinition;

        private TemplateBuildContext(TemplateDefinition template,
                                     String preferredLanguage,
                                     GenerationConfig config,
                                     CdaModelRepository cdaRepository,
                                     Map<String, TemplateDefinition> templateById,
                                     List<IRDiagnostic> diagnostics) {
            this.template = template;
            this.preferredLanguage = preferredLanguage;
            this.config = config;
            this.cdaRepository = cdaRepository;
            this.templateById = templateById;
            this.diagnostics = diagnostics;
        }

        private void setCdaDefinition(CdaStructureDefinition definition) {
            this.cdaDefinition = definition;
        }

        private String normalizePath(String relativePath) {
            if (cdaDefinition == null) {
                return null;
            }
            if (relativePath == null) {
                return null;
            }
            String trimmed = relativePath.trim();
            if (trimmed.isEmpty()) {
                return trimmed;
            }
            String[] segments = trimmed.split("\\.");
            String currentType = cdaDefinition.name();
            List<String> normalizedSegments = new ArrayList<>();
            for (int i = 0; i < segments.length; i++) {
                String segment = normalizeSegment(segments[i]);
                if (segment.isEmpty()) {
                    return null;
                }
                if ("item".equals(segment) && i + 1 < segments.length) {
                    String nextSegment = normalizeSegment(segments[i + 1]);
                    if (!nextSegment.isEmpty()) {
                        String combined = "item." + nextSegment;
                        SegmentResolution combinedResolution = resolveSegment(currentType, combined);
                        if (combinedResolution != null) {
                            normalizedSegments.add(combinedResolution.segment);
                            if (i + 1 < segments.length - 1) {
                                if (combinedResolution.nextType == null) {
                                    return null;
                                }
                                currentType = combinedResolution.nextType;
                            }
                            i++;
                            continue;
                        }
                    }
                }
                SegmentResolution resolution = resolveSegment(currentType, segment);
                if (resolution == null) {
                    return null;
                }
                normalizedSegments.add(resolution.segment);
                if (i < segments.length - 1) {
                    if (resolution.nextType == null) {
                        return null;
                    }
                    currentType = resolution.nextType;
                }
            }
            return String.join(".", normalizedSegments);
        }

        private String normalizeSegment(String segment) {
            String cleaned = segment.trim();
            if (cleaned.startsWith("@")) {
                cleaned = cleaned.substring(1);
            }
            return cleaned;
        }

        private SegmentResolution resolveSegment(String currentType, String segment) {
            Optional<CdaStructureDefinition> currentDefinition = cdaRepository.findByName(currentType);
            if (currentDefinition.isEmpty()) {
                return null;
            }
            Map<String, CdaElementDefinition> elements = currentDefinition.get().elementsByPath();
            String path = currentType + "." + segment;
            CdaElementDefinition element = elements.get(path);
            String normalizedSegment = segment;
            if (element == null) {
                String itemSegment = "item." + segment;
                element = elements.get(currentType + "." + itemSegment);
                if (element != null) {
                    normalizedSegment = itemSegment;
                }
            }
            if (element == null) {
                return null;
            }
            String nextType = resolveTypeName(element.typeCodes());
            return new SegmentResolution(normalizedSegment, nextType);
        }

        private String resolveTypeName(List<String> typeCodes) {
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
                String resolved = resolveKnownType(normalized);
                if (resolved != null) {
                    return resolved;
                }
            }
            return null;
        }

        private String resolveKnownType(String candidate) {
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

        private String rootCdaType() {
            return cdaDefinition != null ? cdaDefinition.name() : null;
        }

        private void addDiagnostic(IRDiagnosticSeverity severity, String path, String message) {
            diagnostics.add(new IRDiagnostic(severity, template.getId(), path, message));
        }

        private String resolveValueSet(String oid) {
            if (oid == null || oid.isBlank()) {
                return null;
            }
            if (oid.startsWith("http") || oid.startsWith("urn:")) {
                return oid;
            }
            String mapped = config.valueSetPolicy().oidToCanonical().get(oid);
            if (mapped != null) {
                return mapped;
            }
            if (config.valueSetPolicy().useOidAsCanonical()) {
                addDiagnostic(IRDiagnosticSeverity.WARNING, null, "Unmapped ValueSet OID: " + oid);
                return "urn:oid:" + oid;
            }
            return null;
        }

        private IRBindingStrength defaultBindingStrength() {
            return config.valueSetPolicy().defaultStrength();
        }
    }

    private static final class ElementConstraintBuilder {
        private final String path;
        private IRCardinality cardinality;
        private String datatype;
        private String fixedValue;
        private IRFixedValueType fixedValueType;
        private List<IRBinding> bindings;
        private String shortDescription;

        private ElementConstraintBuilder(String path) {
            this.path = path;
        }

        private void applyCardinality(IRCardinality newCardinality, TemplateBuildContext context) {
            if (this.cardinality != null && !this.cardinality.equals(newCardinality)) {
                context.addDiagnostic(IRDiagnosticSeverity.WARNING, path, "Conflicting cardinalities");
                return;
            }
            this.cardinality = newCardinality;
        }

        private void applyDatatype(QName datatype) {
            if (datatype == null) {
                return;
            }
            this.datatype = datatype.getLocalPart();
        }

        private void applyFixedValue(String value, QName datatype, String attributeName) {
            if (value == null) {
                return;
            }
            if (this.fixedValue != null && !this.fixedValue.equals(value)) {
                return;
            }
            this.fixedValue = value;
            this.fixedValueType = determineFixedValueType(datatype, attributeName);
        }

        private void applyBindings(List<IRBinding> newBindings, TemplateBuildContext context) {
            if (newBindings.isEmpty()) {
                return;
            }
            if (bindings == null) {
                bindings = new ArrayList<>();
            }
            if (!bindings.isEmpty()) {
                context.addDiagnostic(IRDiagnosticSeverity.WARNING, path, "Multiple bindings found; keeping first");
                return;
            }
            bindings.addAll(newBindings);
        }

        private void applyShortDescription(String description) {
            if (description == null || description.isBlank()) {
                return;
            }
            if (shortDescription == null) {
                shortDescription = description;
            }
        }

        private IRElementConstraint build() {
            return new IRElementConstraint(path, cardinality, datatype, fixedValue, fixedValueType,
                    bindings == null ? List.of() : List.copyOf(bindings), shortDescription);
        }

        private IRFixedValueType determineFixedValueType(QName datatype, String attributeName) {
            String data = datatype != null ? datatype.getLocalPart() : null;
            String name = attributeName == null ? "" : attributeName;
            if (data != null) {
                String upper = data.toUpperCase(Locale.ROOT);
                if (upper.equals("BL") || upper.equals("BOOLEAN") || upper.equals("BOOL") || upper.startsWith("BL.")) {
                    return IRFixedValueType.BOOLEAN;
                }
                if (upper.equals("CS") || upper.equals("CE") || upper.equals("CD") || upper.equals("CV")) {
                    return IRFixedValueType.CODE;
                }
            }
            if (name.endsWith("Ind") || name.endsWith("Indicator")) {
                return IRFixedValueType.BOOLEAN;
            }
            if (name.endsWith("Code") || name.equals("code") || name.equals("classCode") || name.equals("moodCode") || name.equals("typeCode")) {
                return IRFixedValueType.CODE;
            }
            return IRFixedValueType.STRING;
        }
    }
}
