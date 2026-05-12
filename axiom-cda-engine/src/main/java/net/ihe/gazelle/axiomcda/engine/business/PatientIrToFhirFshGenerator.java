package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.ir.IRCardinality;
import net.ihe.gazelle.axiomcda.api.ir.IRElementConstraint;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PatientIrToFhirFshGenerator extends AbstractStructureMapIrToFhirFshGenerator {

    private static final Map<String, String> COMMON_CDA_TO_FHIR_PATH_SEGMENTS = Map.ofEntries(
            Map.entry("addr", "address"),
            Map.entry("additionalLocator", "line"),
            Map.entry("administrativeGenderCode", "gender"),
            Map.entry("birthTime", "birthDate"),
            Map.entry("city", "city"),
            Map.entry("code", "code"),
            Map.entry("country", "country"),
            Map.entry("family", "family"),
            Map.entry("given", "given"),
            Map.entry("houseNumber", "line"),
            Map.entry("id", "identifier"),
            Map.entry("name", "name"),
            Map.entry("postalCode", "postalCode"),
            Map.entry("prefix", "prefix"),
            Map.entry("state", "state"),
            Map.entry("streetName", "line"),
            Map.entry("suffix", "suffix"),
            Map.entry("telecom", "telecom"),
            Map.entry("use", "use")
    );
    private static final Map<String, String> COMMON_IDENTIFIER_CHILD_SEGMENTS = Map.of(
            "extension", "value",
            "root", "system"
    );
    private static final Set<String> COMMON_FHIR_ROOT_PATHS = Set.of(
            "address",
            "birthDate",
            "gender",
            "identifier",
            "managingOrganization",
            "name",
            "telecom"
    );
    private static final Set<String> COMMON_IGNORABLE_CDA_CONTAINERS = Set.of(
            "patient",
            "patientRole",
            "item",
            "place"
    );

    @Override
    protected String resolveAdditionalTargetPath(IRElementConstraint element,
                                                 Map<String, List<SemanticRule>> rulesBySourcePath) {
        InferredTargetPath inferredTarget = inferCommonTarget(element.path(), rulesBySourcePath);
        return inferredTarget == null ? null : inferredTarget.targetPath();
    }

    @Override
    protected List<SemanticRule> additionalUsedRules(IRElementConstraint element,
                                                     Map<String, List<SemanticRule>> rulesBySourcePath,
                                                     String resolvedTargetPath) {
        InferredTargetPath inferredTarget = inferCommonTarget(element.path(), rulesBySourcePath);
        return inferredTarget == null ? List.of() : inferredTarget.usedRules();
    }

    @Override
    protected IRCardinality normalizeCardinality(String targetPath, IRCardinality cardinality) {
        if (targetPath == null || cardinality == null) {
            return cardinality;
        }
        if ("name.family".equals(targetPath) && "*".equals(cardinality.max())) {
            return new IRCardinality(cardinality.min(), "1");
        }
        return cardinality;
    }

    private InferredTargetPath inferCommonTarget(String sourcePath, Map<String, List<SemanticRule>> rulesBySourcePath) {
        if (sourcePath == null || sourcePath.isBlank() || !sourcePath.contains(".")) {
            return null;
        }
        InferredTargetPath ancestorTarget = inferFromNearestMappedAncestor(sourcePath, rulesBySourcePath);
        if (ancestorTarget != null) {
            return ancestorTarget;
        }
        String branch = rootSegment(sourcePath);
        if (!hasMappedDescendants(branch, rulesBySourcePath)) {
            return null;
        }
        String targetPath = inferFromCommonSuffix(sourcePath);
        return targetPath == null ? null : new InferredTargetPath(targetPath, List.of());
    }

    private InferredTargetPath inferFromNearestMappedAncestor(String sourcePath,
                                                              Map<String, List<SemanticRule>> rulesBySourcePath) {
        String prefix = sourcePath;
        while (prefix.contains(".")) {
            prefix = prefix.substring(0, prefix.lastIndexOf('.'));
            List<SemanticRule> rules = rulesBySourcePath.getOrDefault(prefix, List.of());
            String ancestorTargetPath = resolveTargetPathForRules(rules);
            if (ancestorTargetPath == null) {
                continue;
            }
            String remaining = sourcePath.substring(prefix.length() + 1);
            String translatedRemaining = translateRemainingPath(ancestorTargetPath, remaining);
            if (translatedRemaining == null || translatedRemaining.isBlank()) {
                return new InferredTargetPath(ancestorTargetPath, rules);
            }
            return new InferredTargetPath(appendPath(ancestorTargetPath, translatedRemaining), rules);
        }
        return null;
    }

    private String inferFromCommonSuffix(String sourcePath) {
        List<SegmentTranslation> translations = translateSegmentPairs(null, sourcePath);
        List<String> translatedSegments = translations.stream()
                .map(SegmentTranslation::translated)
                .filter(Objects::nonNull)
                .toList();
        for (int i = 0; i < translations.size(); i++) {
            String segment = translations.get(i).translated();
            if (segment == null || !COMMON_FHIR_ROOT_PATHS.contains(segment)) {
                continue;
            }
            if (!leadingSegmentsAreIgnorable(translations, i)) {
                continue;
            }
            int translatedIndex = countTranslatedSegmentsBefore(translations, i);
            return String.join(".", translatedSegments.subList(translatedIndex, translatedSegments.size()));
        }
        return null;
    }

    private String translateRemainingPath(String ancestorTargetPath, String path) {
        List<String> translated = translateSegments(ancestorTargetPath, path);
        return translated.isEmpty() ? null : String.join(".", translated);
    }

    private List<String> translateSegments(String ancestorTargetPath, String path) {
        return translateSegmentPairs(ancestorTargetPath, path).stream()
                .map(SegmentTranslation::translated)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<SegmentTranslation> translateSegmentPairs(String ancestorTargetPath, String path) {
        if (path == null || path.isBlank()) {
            return List.of();
        }
        List<SegmentTranslation> translated = new ArrayList<>();
        String ancestorRoot = rootSegment(ancestorTargetPath);
        for (String segment : path.split("\\.")) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            if ("item".equals(segment)) {
                translated.add(new SegmentTranslation(segment, null));
                continue;
            }
            if ("identifier".equals(ancestorRoot)) {
                String identifierChild = COMMON_IDENTIFIER_CHILD_SEGMENTS.get(segment);
                if (identifierChild != null) {
                    translated.add(new SegmentTranslation(segment, identifierChild));
                    continue;
                }
                return List.of();
            }
            if ("qualifier".equals(segment)) {
                return List.of();
            }
            translated.add(new SegmentTranslation(segment, COMMON_CDA_TO_FHIR_PATH_SEGMENTS.getOrDefault(segment, segment)));
        }
        return translated;
    }

    private boolean leadingSegmentsAreIgnorable(List<SegmentTranslation> translations, int rootIndex) {
        for (int i = 0; i < rootIndex; i++) {
            String translated = translations.get(i).translated();
            if (translated == null) {
                continue;
            }
            if (!COMMON_IGNORABLE_CDA_CONTAINERS.contains(translations.get(i).raw())) {
                return false;
            }
        }
        return true;
    }

    private int countTranslatedSegmentsBefore(List<SegmentTranslation> translations, int endExclusive) {
        int count = 0;
        for (int i = 0; i < endExclusive; i++) {
            if (translations.get(i).translated() != null) {
                count++;
            }
        }
        return count;
    }

    private String appendPath(String base, String suffix) {
        if (base == null || base.isBlank()) {
            return suffix;
        }
        if (suffix == null || suffix.isBlank()) {
            return base;
        }
        if (base.endsWith("." + suffix) || base.equals(suffix)) {
            return base;
        }
        return base + "." + suffix;
    }

    private record InferredTargetPath(String targetPath, List<SemanticRule> usedRules) {
        private InferredTargetPath {
            usedRules = usedRules == null ? List.of() : List.copyOf(usedRules);
        }
    }

    private record SegmentTranslation(String raw, String translated) {
    }
}
