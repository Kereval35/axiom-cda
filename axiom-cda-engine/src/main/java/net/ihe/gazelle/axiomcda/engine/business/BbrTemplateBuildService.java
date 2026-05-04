package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.bbr.Assert;
import net.ihe.gazelle.axiomcda.api.bbr.Attribute;
import net.ihe.gazelle.axiomcda.api.bbr.ChoiceDefinition;
import net.ihe.gazelle.axiomcda.api.bbr.CodingStrengthType;
import net.ihe.gazelle.axiomcda.api.bbr.IncludeDefinition;
import net.ihe.gazelle.axiomcda.api.bbr.RuleDefinition;
import net.ihe.gazelle.axiomcda.api.bbr.TemplateDefinition;
import net.ihe.gazelle.axiomcda.api.bbr.Vocabulary;
import net.ihe.gazelle.axiomcda.api.cda.CdaStructureDefinition;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.ir.*;
import net.ihe.gazelle.axiomcda.api.port.CdaModelRepository;
import net.ihe.gazelle.axiomcda.engine.util.TextUtil;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class BbrTemplateBuildService {
    private static final Pattern COUNT_ASSERT_PATTERN = Pattern.compile("count\\(([^)]+)\\)\\s*(=|>=|<=|>)\\s*(\\d+)");

    IRTemplate buildTemplate(TemplateDefinition template,
                             String preferredLanguage,
                             GenerationConfig config,
                             CdaModelRepository cdaRepository,
                             Map<String, TemplateDefinition> templateById,
                             List<IRDiagnostic> diagnostics,
                             IRTemplateOrigin origin) {
        TemplateBuildContext context = new TemplateBuildContext(template, preferredLanguage, config, cdaRepository, templateById, diagnostics);
        RuleDefinition rootRule = findRootRule(template, cdaRepository);
        if (rootRule == null || rootRule.getName() == null) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, null, "No root element found");
            return null;
        }

        ParsedName rootName = BbrNameParser.parseElementName(rootRule.getName());
        if (rootName == null || rootName.baseName() == null || rootName.baseName().isBlank()) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, null, "Invalid root element name");
            return null;
        }

        if (rootName.predicate() != null) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, rootName.baseName(), "Root element predicate ignored");
        }

        String rootCdaType = BbrNameParser.resolveRootCdaType(rootName.baseName());
        Optional<CdaStructureDefinition> cdaDefinition = cdaRepository.findByName(rootCdaType);
        if (cdaDefinition.isEmpty()) {
            context.addDiagnostic(IRDiagnosticSeverity.ERROR, rootName.baseName(), "CDA structure definition not found for root type");
            return null;
        }
        context.setCdaDefinition(cdaDefinition.get());

        Map<String, ElementConstraintBuilder> builders = new LinkedHashMap<>();
        List<IRTemplateInclude> includes = new ArrayList<>();
        List<IRInvariant> invariants = new ArrayList<>();

        String templateDescription = TextUtil.selectDescription(template.getDesc(), preferredLanguage);

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
                invariants,
                origin
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
        ParsedName parsed = BbrNameParser.parseElementName(rule.getName());
        if (parsed == null || parsed.baseName() == null || parsed.baseName().isBlank()) {
            return;
        }
        if (parsed.predicate() != null) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, parsed.baseName(), "Predicated element not supported (slicing skipped)");
            return;
        }
        String currentPath = parentPath.isEmpty() ? parsed.baseName() : parentPath + "." + parsed.baseName();
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
        String shortDesc = TextUtil.selectDescription(rule.getDesc(), context.preferredLanguage());
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
            String normalized = BbrNameParser.normalizeName(attribute.getName());
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
            String shortDesc = TextUtil.selectDescription(attribute.getDesc(), context.preferredLanguage());
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
        TemplateDefinition includedTemplate = context.templateById().get(ref);
        if (includedTemplate == null) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, parentPath, "Included template not found: " + ref);
            return;
        }
        RuleDefinition includedRoot = findRootRule(includedTemplate, context.cdaRepository());
        if (includedRoot == null || includedRoot.getName() == null) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, parentPath, "Included template missing root element: " + ref);
            return;
        }
        ParsedName includeName = BbrNameParser.parseElementName(includedRoot.getName());
        if (includeName == null || includeName.baseName() == null || includeName.baseName().isBlank()) {
            return;
        }
        if (includeName.predicate() != null) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, parentPath, "Included template root predicate ignored");
        }
        String includePath = parentPath.isEmpty() ? includeName.baseName() : parentPath + "." + includeName.baseName();
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
        String normalizedPath = BbrNameParser.normalizeInvariantPath(rawPath, context.rootCdaType());
        if (normalizedPath == null || normalizedPath.isBlank()) {
            return Optional.empty();
        }
        String mappedPath = context.normalizePath(normalizedPath);
        if (mappedPath != null) {
            normalizedPath = mappedPath;
        }
        String expression = normalizedPath + ".count() " + operator + " " + count;
        String description = TextUtil.flattenMixedContent(assertion.getContent());
        String prefix = context.config().naming().profilePrefix();
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

    private RuleDefinition findRootRule(TemplateDefinition template, CdaModelRepository cdaRepository) {
        if (template.getAttributeOrChoiceOrElement() == null) {
            return null;
        }
        RuleDefinition fallback = null;
        for (Object entry : template.getAttributeOrChoiceOrElement()) {
            if (entry instanceof RuleDefinition rule && rule.getName() != null) {
                if (fallback == null) {
                    fallback = rule;
                }
                if (cdaRepository == null || isCdaRoot(rule.getName(), cdaRepository)) {
                    return rule;
                }
            }
            if (entry instanceof ChoiceDefinition choice && choice.getIncludeOrElementOrConstraint() != null) {
                for (Object choiceEntry : choice.getIncludeOrElementOrConstraint()) {
                    if (choiceEntry instanceof RuleDefinition rule && rule.getName() != null) {
                        if (fallback == null) {
                            fallback = rule;
                        }
                        if (cdaRepository == null || isCdaRoot(rule.getName(), cdaRepository)) {
                            return rule;
                        }
                    }
                }
            }
        }
        return fallback;
    }

    private boolean isCdaRoot(String name, CdaModelRepository cdaRepository) {
        ParsedName parsed = BbrNameParser.parseElementName(name);
        if (parsed == null || parsed.baseName() == null || parsed.baseName().isBlank()) {
            return false;
        }
        String rootType = BbrNameParser.resolveRootCdaType(parsed.baseName());
        return cdaRepository.findByName(rootType).isPresent();
    }
}
