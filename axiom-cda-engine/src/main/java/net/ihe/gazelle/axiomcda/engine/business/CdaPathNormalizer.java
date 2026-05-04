package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.cda.CdaElementDefinition;
import net.ihe.gazelle.axiomcda.api.cda.CdaStructureDefinition;
import net.ihe.gazelle.axiomcda.api.port.CdaModelRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class CdaPathNormalizer {
    private CdaPathNormalizer() {
    }

    static String normalizePath(String relativePath,
                                CdaStructureDefinition cdaDefinition,
                                CdaModelRepository cdaRepository) {
        if (cdaDefinition == null || relativePath == null) {
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
                    PathSegmentResolution combinedResolution = resolveSegment(currentType, combined, cdaRepository);
                    if (combinedResolution != null) {
                        normalizedSegments.add(combinedResolution.segment());
                        if (i + 1 < segments.length - 1) {
                            if (combinedResolution.nextType() == null) {
                                return null;
                            }
                            currentType = combinedResolution.nextType();
                        }
                        i++;
                        continue;
                    }
                }
            }
            PathSegmentResolution resolution = resolveSegment(currentType, segment, cdaRepository);
            if (resolution == null) {
                return null;
            }
            normalizedSegments.add(resolution.segment());
            if (i < segments.length - 1) {
                if (resolution.nextType() == null) {
                    return null;
                }
                currentType = resolution.nextType();
            }
        }
        return String.join(".", normalizedSegments);
    }

    private static String normalizeSegment(String segment) {
        String cleaned = segment.trim();
        if (cleaned.startsWith("@")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned;
    }

    private static PathSegmentResolution resolveSegment(String currentType,
                                                        String segment,
                                                        CdaModelRepository cdaRepository) {
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
        String nextType = resolveTypeName(element.typeCodes(), cdaRepository);
        return new PathSegmentResolution(normalizedSegment, nextType);
    }

    private static String resolveTypeName(List<String> typeCodes, CdaModelRepository cdaRepository) {
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

    private static String resolveKnownType(String candidate, CdaModelRepository cdaRepository) {
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
}
