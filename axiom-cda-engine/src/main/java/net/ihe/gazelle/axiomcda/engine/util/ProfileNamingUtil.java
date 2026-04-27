package net.ihe.gazelle.axiomcda.engine.util;

import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;

import java.util.*;

public final class ProfileNamingUtil {

    private ProfileNamingUtil() {
    }

    public static Map<String, String> resolveProfileNames(List<IRTemplate> templates, GenerationConfig config) {
        return resolveUniqueMap(
                templates,
                template -> config.naming().profileNameOverrides().get(template.id()),
                template -> config.naming().profilePrefix() + buildTemplateNameStem(template),
                true
        );
    }

    public static Map<String, String> resolveProfileIds(List<IRTemplate> templates, GenerationConfig config) {
        return resolveUniqueMap(
                templates,
                template -> config.naming().idOverrides().get(template.id()),
                template -> {
                    String stem = buildTemplateNameStem(template);
                    return config.naming().idPrefix() + NameUtil.toKebabCase(stem);
                },
                false
        );
    }

    public static String resolveTitle(IRTemplate template, GenerationConfig config) {
        String base = firstNonBlank(template.displayName(), template.name(), template.rootCdaType());
        String prefix = config.naming().titlePrefix() == null ? "" : config.naming().titlePrefix();
        return prefix + base;
    }

    private static Map<String, String> resolveUniqueMap(List<IRTemplate> templates,
                                                        java.util.function.Function<IRTemplate, String> overrideResolver,
                                                        java.util.function.Function<IRTemplate, String> defaultResolver,
                                                        boolean pascalSuffix) {
        Map<String, String> values = new LinkedHashMap<>();
        Set<String> used = new HashSet<>();
        for (IRTemplate template : templates) {
            String override = overrideResolver.apply(template);
            String base = firstNonBlank(override, defaultResolver.apply(template));
            if (base == null || base.isBlank()) {
                base = pascalSuffix ? buildTemplateNameStem(template) : NameUtil.toKebabCase(buildTemplateNameStem(template));
            }
            String unique = uniquify(base, template.id(), used, pascalSuffix);
            values.put(template.id(), unique);
            used.add(unique);
        }
        return values;
    }

    private static String uniquify(String base, String templateId, Set<String> used, boolean pascalSuffix) {
        if (!used.contains(base)) {
            return base;
        }
        String templateSuffix = templateIdSuffix(templateId, pascalSuffix);
        String candidate = pascalSuffix ? base + templateSuffix : base + "-" + templateSuffix;
        if (!used.contains(candidate)) {
            return candidate;
        }
        int index = 2;
        while (true) {
            candidate = pascalSuffix ? base + templateSuffix + index : base + "-" + templateSuffix + "-" + index;
            if (!used.contains(candidate)) {
                return candidate;
            }
            index++;
        }
    }

    private static String buildTemplateNameStem(IRTemplate template) {
        String root = NameUtil.toPascalCase(template.rootCdaType());
        String label = firstNonBlank(template.displayName(), template.name());
        String normalizedLabel = NameUtil.toPascalCase(label);
        if (normalizedLabel == null || normalizedLabel.isBlank()) {
            return root;
        }
        if (normalizedLabel.equalsIgnoreCase(root) || normalizedLabel.startsWith(root)) {
            return normalizedLabel;
        }
        return root + normalizedLabel;
    }

    private static String templateIdSuffix(String templateId, boolean pascalSuffix) {
        if (templateId == null || templateId.isBlank()) {
            return pascalSuffix ? "Template" : "template";
        }
        String[] parts = NameUtil.normalizeAscii(templateId).split("[^A-Za-z0-9]+");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            if (part != null && !part.isBlank()) {
                if (pascalSuffix) {
                    return NameUtil.toPascalCase(part);
                }
                return NameUtil.toKebabCase(part);
            }
        }
        return pascalSuffix ? "Template" : "template";
    }

    private static String firstNonBlank(String... values) {
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
}
