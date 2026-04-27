package net.ihe.gazelle.axiomcda.ws.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

class ParentProfileFshNormalizer {

    private static final String R4_CORE_PACKAGE_ID = "hl7.fhir.r4.core";
    private static final String R4_CORE_VERSION = "4.0.1";
    private static final String BASE_OBSERVATION_URL = "http://hl7.org/fhir/StructureDefinition/Observation";
    private static final Pattern CARDINALITY_RULE = Pattern.compile("^(\\s*\\*\\s+)(\\S+)\\s+(\\d+)\\.\\.([^\\s]+)(\\s*)$");

    private final ObjectMapper mapper = new ObjectMapper();

    NormalizationResult normalize(String fshContent,
                                  String parent,
                                  boolean baseObservationParent,
                                  String dependencyPackageId,
                                  String dependencyVersion) {
        if (fshContent == null || fshContent.isBlank() || parent == null || parent.isBlank()) {
            return new NormalizationResult(fshContent, List.of());
        }

        String packageId = baseObservationParent ? R4_CORE_PACKAGE_ID : dependencyPackageId;
        String packageVersion = baseObservationParent ? R4_CORE_VERSION : dependencyVersion;
        Optional<ParentProfile> parentProfile = loadParentProfile(resolveParent(parent), packageId, packageVersion);
        if (parentProfile.isEmpty()) {
            return new NormalizationResult(fshContent, List.of(
                    "Parent-aware normalization skipped: parent profile " + parent
                            + " was not found in local FHIR package cache for "
                            + packageId + "#" + packageVersion + "."
            ));
        }

        List<String> diagnostics = new ArrayList<>();
        List<String> normalizedLines = new ArrayList<>();
        for (String line : fshContent.split("\\R", -1)) {
            Matcher matcher = CARDINALITY_RULE.matcher(line);
            if (!matcher.matches()) {
                normalizedLines.add(line);
                continue;
            }

            String targetPath = matcher.group(2);
            ParentElement parentElement = parentProfile.get().elementsByPath().get(targetPath);
            if (parentElement == null) {
                normalizedLines.add(line);
                continue;
            }

            int originalMin = Integer.parseInt(matcher.group(3));
            String originalMax = matcher.group(4);
            int normalizedMin = Math.max(originalMin, parentElement.min());
            String normalizedMax = narrowerMax(originalMax, parentElement.max());
            if (!"*".equals(normalizedMax) && Integer.parseInt(normalizedMax) < normalizedMin) {
                normalizedMax = String.valueOf(normalizedMin);
            }

            if (originalMin != normalizedMin || !originalMax.equals(normalizedMax)) {
                diagnostics.add("Adjusted " + targetPath + " cardinality from "
                        + originalMin + ".." + originalMax + " to "
                        + normalizedMin + ".." + normalizedMax
                        + " to fit parent " + parentProfile.get().url() + ".");
            }
            normalizedLines.add(matcher.group(1) + targetPath + " " + normalizedMin + ".." + normalizedMax + matcher.group(5));
        }

        return new NormalizationResult(String.join("\n", normalizedLines), diagnostics);
    }

    private String resolveParent(String parent) {
        return "Observation".equals(parent) ? BASE_OBSERVATION_URL : parent;
    }

    private Optional<ParentProfile> loadParentProfile(String parentUrl, String packageId, String packageVersion) {
        if (isBlank(packageId) || isBlank(packageVersion)) {
            return Optional.empty();
        }
        Optional<Path> packageDir = findPackageDir(packageId, packageVersion);
        if (packageDir.isEmpty()) {
            return Optional.empty();
        }

        try (Stream<Path> files = Files.list(packageDir.get())) {
            List<Path> structureDefinitions = files
                    .filter(path -> path.getFileName().toString().startsWith("StructureDefinition-"))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .toList();
            for (Path structureDefinition : structureDefinitions) {
                JsonNode root = mapper.readTree(structureDefinition.toFile());
                if (!"StructureDefinition".equals(root.path("resourceType").asText())) {
                    continue;
                }
                if (matchesParent(root, parentUrl)) {
                    return Optional.of(new ParentProfile(root.path("url").asText(parentUrl), readElements(root)));
                }
            }
        } catch (IOException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private Optional<Path> findPackageDir(String packageId, String packageVersion) {
        String packageDirectoryName = packageId + "#" + packageVersion;
        for (Path cacheRoot : cacheRoots()) {
            Path packageDir = cacheRoot.resolve(packageDirectoryName).resolve("package");
            if (Files.isDirectory(packageDir)) {
                return Optional.of(packageDir);
            }
        }
        return Optional.empty();
    }

    private List<Path> cacheRoots() {
        List<Path> roots = new ArrayList<>();
        addEnvPath(roots, "FHIR_PACKAGE_CACHE");
        addEnvPath(roots, "FHIR_PACKAGES_CACHE");
        String userHome = System.getProperty("user.home");
        if (!isBlank(userHome)) {
            roots.add(Path.of(userHome, ".fhir", "packages"));
        }
        roots.add(Path.of(System.getProperty("java.io.tmpdir"), ".fhir", "packages"));
        return roots;
    }

    private void addEnvPath(List<Path> roots, String envName) {
        String value = System.getenv(envName);
        if (!isBlank(value)) {
            roots.add(Path.of(value));
        }
    }

    private boolean matchesParent(JsonNode root, String parentUrl) {
        return parentUrl.equals(root.path("url").asText())
                || parentUrl.equals(root.path("id").asText())
                || parentUrl.equals(root.path("name").asText());
    }

    private Map<String, ParentElement> readElements(JsonNode root) {
        Map<String, ParentElement> elementsByPath = new LinkedHashMap<>();
        JsonNode elements = root.path("snapshot").path("element");
        if (!elements.isArray()) {
            return elementsByPath;
        }
        for (JsonNode element : elements) {
            String path = element.path("path").asText(null);
            if (isBlank(path)) {
                continue;
            }
            String relativePath = path.contains(".") ? path.substring(path.indexOf('.') + 1) : path;
            if (relativePath.isBlank()) {
                continue;
            }
            elementsByPath.putIfAbsent(relativePath, new ParentElement(
                    element.path("min").asInt(0),
                    element.path("max").asText("*")
            ));
        }
        return elementsByPath;
    }

    private String narrowerMax(String first, String second) {
        if ("*".equals(first)) {
            return second;
        }
        if ("*".equals(second)) {
            return first;
        }
        return String.valueOf(Math.min(Integer.parseInt(first), Integer.parseInt(second)));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    record NormalizationResult(String fshContent, List<String> diagnostics) {
    }

    private record ParentProfile(String url, Map<String, ParentElement> elementsByPath) {
    }

    private record ParentElement(int min, String max) {
    }
}
