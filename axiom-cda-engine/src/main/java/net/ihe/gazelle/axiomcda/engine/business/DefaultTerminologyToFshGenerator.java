package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.bbr.*;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.fsh.FshBundle;
import net.ihe.gazelle.axiomcda.engine.util.FshUtil;
import net.ihe.gazelle.axiomcda.engine.util.TextUtil;

import java.text.Normalizer;
import java.util.*;

public class DefaultTerminologyToFshGenerator {
    private static final String VALUESETS_DIR = "ValueSets";
    private static final String CODESYSTEMS_DIR = "CodeSystems";

    public FshBundle generate(Decor decor, GenerationConfig config) {
        if (decor == null || decor.getTerminology() == null) {
            return new FshBundle(Map.of());
        }
        Terminology terminology = decor.getTerminology();
        String preferredLanguage = decor.getProject() != null ? decor.getProject().getDefaultLanguage() : decor.getLanguage();

        Map<String, String> files = new LinkedHashMap<>();
        NameRegistry registry = new NameRegistry();

        for (CodeSystem codeSystem : terminology.getCodeSystem()) {
            String fsh = buildCodeSystem(codeSystem, preferredLanguage, config, registry);
            if (fsh == null) {
                continue;
            }
            String id = registry.lastId();
            files.put(CODESYSTEMS_DIR + "/" + id + ".fsh", fsh);
        }

        for (ValueSet valueSet : terminology.getValueSet()) {
            String fsh = buildValueSet(valueSet, preferredLanguage, config, registry);
            if (fsh == null) {
                continue;
            }
            String id = registry.lastId();
            files.put(VALUESETS_DIR + "/" + id + ".fsh", fsh);
        }

        return new FshBundle(files);
    }

    private String buildCodeSystem(CodeSystem codeSystem,
                                   String preferredLanguage,
                                   GenerationConfig config,
                                   NameRegistry registry) {
        if (codeSystem == null) {
            return null;
        }
        String rawName = firstNonBlank(codeSystem.getName(), codeSystem.getDisplayName(),
                codeSystem.getId(), codeSystem.getRef());
        if (rawName == null) {
            return null;
        }
        String name = registry.uniqueName(toFshName(rawName, "CodeSystem"));
        String id = registry.uniqueId(toFshId(rawName, "code-system"));
        registry.setLastId(id);

        String title = firstNonBlank(codeSystem.getDisplayName(), codeSystem.getName(), rawName);
        String description = TextUtil.selectDescription(codeSystem.getDesc(), preferredLanguage);
        String canonical = canonicalFromOid(firstNonBlank(codeSystem.getId(), codeSystem.getRef()), config);
        String status = mapStatus(codeSystem.getStatusCode());
        String version = codeSystem.getVersionLabel();

        StringBuilder builder = new StringBuilder();
        builder.append("CodeSystem: ").append(name).append("\n");
        builder.append("Id: ").append(id).append("\n");
        builder.append("Title: \"").append(FshUtil.escape(title)).append("\"\n");
        if (description != null) {
            builder.append("Description: \"").append(FshUtil.escape(description)).append("\"\n");
        }
        if (canonical != null) {
            builder.append("* ^url = \"").append(FshUtil.escape(canonical)).append("\"\n");
        }
        if (version != null && !version.isBlank()) {
            builder.append("* ^version = \"").append(FshUtil.escape(version)).append("\"\n");
        }
        builder.append("* ^status = #").append(status).append("\n");

        CodeSystemConceptList conceptList = codeSystem.getConceptList();
        if (conceptList != null && !conceptList.getCodedConcept().isEmpty()) {
            builder.append("* ^content = #complete\n");
            Set<String> emitted = new HashSet<>();
            for (CodedConcept concept : conceptList.getCodedConcept()) {
                if (concept == null || concept.getCode() == null || concept.getCode().isBlank()) {
                    continue;
                }
                String codeKey = concept.getCode().trim();
                if (!emitted.add(codeKey)) {
                    continue;
                }
                String display = selectDesignation(concept.getDesignation(), preferredLanguage);
                if (display == null) {
                    display = concept.getCode();
                }
                builder.append("* ").append(formatCodeToken(concept.getCode())).append(" \"")
                        .append(FshUtil.escape(display)).append("\"\n");
            }
        } else {
            builder.append("* ^content = #not-present\n");
        }
        return builder.toString();
    }

    private String buildValueSet(ValueSet valueSet,
                                 String preferredLanguage,
                                 GenerationConfig config,
                                 NameRegistry registry) {
        if (valueSet == null) {
            return null;
        }
        String rawName = firstNonBlank(valueSet.getName(), valueSet.getDisplayName(),
                valueSet.getId(), valueSet.getRef());
        if (rawName == null) {
            return null;
        }
        String name = registry.uniqueName(toFshName(rawName, "ValueSet"));
        String id = registry.uniqueId(toFshId(rawName, "value-set"));
        registry.setLastId(id);

        String title = firstNonBlank(valueSet.getDisplayName(), valueSet.getName(), rawName);
        String description = TextUtil.selectDescription(valueSet.getDesc(), preferredLanguage);
        String canonical = canonicalFromOid(firstNonBlank(valueSet.getId(), valueSet.getRef()), config);
        String status = mapStatus(valueSet.getStatusCode());
        String version = valueSet.getVersionLabel();

        StringBuilder builder = new StringBuilder();
        builder.append("ValueSet: ").append(name).append("\n");
        builder.append("Id: ").append(id).append("\n");
        builder.append("Title: \"").append(FshUtil.escape(title)).append("\"\n");
        if (description != null) {
            builder.append("Description: \"").append(FshUtil.escape(description)).append("\"\n");
        }
        if (canonical != null) {
            builder.append("* ^url = \"").append(FshUtil.escape(canonical)).append("\"\n");
        }
        if (version != null && !version.isBlank()) {
            builder.append("* ^version = \"").append(FshUtil.escape(version)).append("\"\n");
        }
        builder.append("* ^status = #").append(status).append("\n");

        Set<String> emitted = new HashSet<>();
        for (CodeSystemReference systemRef : valueSet.getCompleteCodeSystem()) {
            String system = canonicalFromOid(systemRef.getCodeSystem(), config);
            if (system != null && emitted.add("include|system|" + system)) {
                builder.append("* include codes from system ").append(system).append("\n");
            }
        }

        ValueSetConceptList conceptList = valueSet.getConceptList();
        if (conceptList != null) {
            for (Object entry : conceptList.getConceptOrInclude()) {
                if (entry instanceof ValueSetConcept concept) {
                    appendValueSetConcept(builder, concept, config, preferredLanguage, "include", emitted);
                } else if (entry instanceof ValueSetRef ref) {
                    appendValueSetReference(builder, ref, config, emitted);
                }
            }
            for (ValueSetConcept exception : conceptList.getException()) {
                appendValueSetConcept(builder, exception, config, preferredLanguage, "exclude", emitted);
            }
        }

        return builder.toString();
    }

    private void appendValueSetReference(StringBuilder builder,
                                         ValueSetRef ref,
                                         GenerationConfig config,
                                         Set<String> emitted) {
        if (ref == null || ref.getRef() == null || ref.getRef().isBlank()) {
            return;
        }
        String canonical = canonicalFromOid(ref.getRef(), config);
        if (canonical == null) {
            return;
        }
        String verb = ref.isException() ? "exclude" : "include";
        if (!emitted.add(verb + "|valueset|" + canonical)) {
            return;
        }
        builder.append("* ").append(verb).append(" codes from valueset ").append(canonical).append("\n");
    }

    private void appendValueSetConcept(StringBuilder builder,
                                       ValueSetConcept concept,
                                       GenerationConfig config,
                                       String preferredLanguage,
                                       String verb,
                                       Set<String> emitted) {
        if (concept == null || concept.getCode() == null || concept.getCode().isBlank()) {
            return;
        }
        String system = canonicalFromOid(concept.getCodeSystem(), config);
        if (system == null) {
            return;
        }
        String code = concept.getCode().trim();
        if (!emitted.add(verb + "|code|" + system + "|" + code)) {
            return;
        }
        String display = concept.getDisplayName();
        if (display == null || display.isBlank()) {
            display = TextUtil.selectDescription(concept.getDesc(), preferredLanguage);
        }
        builder.append("* ").append(verb).append(" ").append(system).append(formatCodeToken(code));
        if (display != null && !display.isBlank()) {
            builder.append(" \"").append(FshUtil.escape(display)).append("\"");
        }
        builder.append("\n");
    }

    private String canonicalFromOid(String oid, GenerationConfig config) {
        if (oid == null || oid.isBlank()) {
            return null;
        }
        if (oid.startsWith("http") || oid.startsWith("urn:")) {
            return oid;
        }
        String mapped = config != null ? config.valueSetPolicy().oidToCanonical().get(oid) : null;
        if (mapped != null && !mapped.isBlank()) {
            return mapped;
        }
        return "urn:oid:" + oid;
    }

    private String mapStatus(ItemStatusCodeLifeCycle status) {
        if (status == null) {
            return "active";
        }
        return switch (status) {
            case NEW, DRAFT, PENDING -> "draft";
            case FINAL -> "active";
            case REJECTED, CANCELLED, DEPRECATED -> "retired";
        };
    }

    private String selectDesignation(List<Designation> designations, String preferredLanguage) {
        if (designations == null || designations.isEmpty()) {
            return null;
        }
        for (Designation designation : designations) {
            if (designation == null) {
                continue;
            }
            String displayName = designation.getDisplayName();
            if (displayName == null || displayName.isBlank()) {
                continue;
            }
            if (preferredLanguage != null
                    && designation.getLanguage() != null
                    && preferredLanguage.equalsIgnoreCase(designation.getLanguage())) {
                return displayName;
            }
        }
        for (Designation designation : designations) {
            if (designation == null) {
                continue;
            }
            String displayName = designation.getDisplayName();
            if (displayName != null && !displayName.isBlank()) {
                return displayName;
            }
        }
        return null;
    }

    private String formatCodeToken(String code) {
        String trimmed = code == null ? "" : code.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.matches("[A-Za-z0-9\\-\\.]+")) {
            return "#" + trimmed;
        }
        return "#\"" + FshUtil.escape(trimmed) + "\"";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String toFshName(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String cleaned = normalized.replaceAll("[^A-Za-z0-9]+", " ").trim();
        if (cleaned.isEmpty()) {
            return fallback;
        }
        StringBuilder builder = new StringBuilder();
        for (String part : cleaned.split("\\s+")) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        String result = builder.toString();
        if (result.isEmpty()) {
            return fallback;
        }
        if (!Character.isLetter(result.charAt(0))) {
            return fallback + result;
        }
        return result;
    }

    private String toFshId(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String cleaned = normalized.replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "")
                .toLowerCase(Locale.ROOT);
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private static final class NameRegistry {
        private final Map<String, Integer> nameCounts = new HashMap<>();
        private final Map<String, Integer> idCounts = new HashMap<>();
        private String lastId;

        String uniqueName(String base) {
            return unique(base, nameCounts);
        }

        String uniqueId(String base) {
            return unique(base, idCounts);
        }

        void setLastId(String id) {
            this.lastId = id;
        }

        String lastId() {
            return lastId;
        }

        private String unique(String base, Map<String, Integer> counts) {
            String safeBase = base == null || base.isBlank() ? "Generated" : base;
            int next = counts.getOrDefault(safeBase, 0) + 1;
            counts.put(safeBase, next);
            if (next == 1) {
                return safeBase;
            }
            return safeBase + next;
        }
    }
}
