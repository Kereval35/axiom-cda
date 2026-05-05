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
        CdaStructureDefinition currentDefinition = cdaDefinition;
        String currentPathPrefix = cdaDefinition.name();
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
                    PathSegmentResolution combinedResolution = resolveSegment(currentDefinition,
                            currentPathPrefix,
                            combined,
                            cdaRepository);
                    if (combinedResolution != null) {
                        normalizedSegments.add(combinedResolution.segment());
                        if (i + 1 < segments.length - 1) {
                            CurrentContext nextContext = advanceContext(currentDefinition,
                                    combinedResolution,
                                    cdaRepository);
                            if (nextContext == null) {
                                return null;
                            }
                            currentDefinition = nextContext.definition();
                            currentPathPrefix = nextContext.pathPrefix();
                        }
                        i++;
                        continue;
                    }
                }
            }
            PathSegmentResolution resolution = resolveSegment(currentDefinition,
                    currentPathPrefix,
                    segment,
                    cdaRepository);
            if (resolution == null) {
                return null;
            }
            normalizedSegments.add(resolution.segment());
            if (i < segments.length - 1) {
                CurrentContext nextContext = advanceContext(currentDefinition, resolution, cdaRepository);
                if (nextContext == null) {
                    return null;
                }
                currentDefinition = nextContext.definition();
                currentPathPrefix = nextContext.pathPrefix();
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

    private static PathSegmentResolution resolveSegment(CdaStructureDefinition currentDefinition,
                                                        String currentPathPrefix,
                                                        String segment,
                                                        CdaModelRepository cdaRepository) {
        if (currentDefinition == null || currentPathPrefix == null || currentPathPrefix.isBlank()) {
            return null;
        }
        Map<String, CdaElementDefinition> elements = currentDefinition.elementsByPath();
        String path = currentPathPrefix + "." + segment;
        CdaElementDefinition element = elements.get(path);
        String normalizedSegment = segment;
        if (element == null) {
            String itemSegment = "item." + segment;
            element = elements.get(currentPathPrefix + "." + itemSegment);
            if (element != null) {
                normalizedSegment = itemSegment;
            }
        }
        if (element == null) {
            return null;
        }
        String nextType = resolveTypeName(element.typeCodes(), cdaRepository);
        return new PathSegmentResolution(normalizedSegment, nextType, element.path());
    }

    private static CurrentContext advanceContext(CdaStructureDefinition currentDefinition,
                                                 PathSegmentResolution resolution,
                                                 CdaModelRepository cdaRepository) {
        if (hasDescendants(currentDefinition, resolution.resolvedPath())) {
            return new CurrentContext(currentDefinition, resolution.resolvedPath());
        }
        if (resolution.nextType() == null) {
            return null;
        }
        Optional<CdaStructureDefinition> nextDefinition = cdaRepository.findByName(resolution.nextType());
        if (nextDefinition.isEmpty()) {
            return null;
        }
        return new CurrentContext(nextDefinition.get(), resolution.nextType());
    }

    private static boolean hasDescendants(CdaStructureDefinition definition, String pathPrefix) {
        String nestedPrefix = pathPrefix + ".";
        return definition.elementsByPath().keySet().stream().anyMatch(path -> path.startsWith(nestedPrefix));
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

    private record CurrentContext(CdaStructureDefinition definition, String pathPrefix) {
    }
}
