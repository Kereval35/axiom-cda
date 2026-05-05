package net.ihe.gazelle.axiomcda.ws.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import net.ihe.gazelle.axiomcda.engine.business.GenericIrToFhirFshGenerator;
import net.ihe.gazelle.axiomcda.engine.business.ObservationFhirConversionResult;
import net.ihe.gazelle.axiomcda.engine.business.ObservationIrToFhirFshGenerator;
import net.ihe.gazelle.axiomcda.fhirmappings.api.MappingModelProvider;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;
import net.ihe.gazelle.axiomcda.fhirmappings.builtin.BuiltInMappingModelProvider;
import net.ihe.gazelle.axiomcda.fhirmappings.structuremap.StructureMapUploadModelProvider;
import net.ihe.gazelle.axiomcda.fhirmappings.trace.SemanticMappingFshTraceRenderer;
import net.ihe.gazelle.axiomcda.ws.dto.FhirConversionRequest;
import net.ihe.gazelle.axiomcda.ws.dto.FhirConversionResult;
import net.ihe.gazelle.axiomcda.ws.dto.FshProfile;
import net.ihe.gazelle.axiomcda.ws.dto.SushiCompileRequest;
import net.ihe.gazelle.axiomcda.ws.dto.SushiCompileResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@ApplicationScoped
public class FhirConversionService {

    private static final Duration SUSHI_TIMEOUT = Duration.ofSeconds(120);
    private static final String BASE_OBSERVATION_PARENT = "Observation";
    private static final String BASE_OBSERVATION_URL = "http://hl7.org/fhir/StructureDefinition/Observation";
    private static final String R4_CORE_PACKAGE_ID = "hl7.fhir.r4.core";
    private static final String R4_CORE_VERSION = "4.0.1";

    public FhirConversionResult convertObservation(FhirConversionRequest request) throws Exception {
        if (request == null || request.template() == null) {
            throw new IllegalArgumentException("A selected IR template is required for FHIR conversion");
        }
        boolean observationTemplate = "Observation".equals(request.template().rootCdaType());
        boolean uploadedStructureMap = request.structureMap() != null && !request.structureMap().isBlank();
        if (!observationTemplate && !uploadedStructureMap) {
            throw new BadRequestException("An uploaded StructureMap JSON is required for non-Observation FHIR conversion");
        }
        if (!observationTemplate && request.builtInMappingId() != null && !request.builtInMappingId().isBlank()) {
            throw new BadRequestException("Built-in FHIR mapping presets are only available for Observation templates");
        }

        MappingModelProvider provider = !uploadedStructureMap
                ? new BuiltInMappingModelProvider()
                : new StructureMapUploadModelProvider(request.structureMap());
        SemanticMappingModel mappingModel = provider instanceof BuiltInMappingModelProvider builtInProvider
                ? builtInProvider.resolve(request.template().rootCdaType(), request.builtInMappingId())
                : provider.resolve(request.template().rootCdaType());
        ObservationFhirConversionResult conversion = observationTemplate
                ? new ObservationIrToFhirFshGenerator().generate(
                        request.template(),
                        request.sourceProfileName(),
                        mappingModel
                )
                : new GenericIrToFhirFshGenerator().generate(
                        request.template(),
                        request.sourceProfileName(),
                        mappingModel
                );
        String mappingRulesName = conversion.profileName() + "MappingRules";
        String mappingRulesFsh = new SemanticMappingFshTraceRenderer().render(
                mappingRulesName,
                request.template().rootCdaType(),
                resolveMappingSourceDescription(request),
                mappingModel
        );
        String usedMappingRulesName = conversion.profileName() + "UsedMappingRules";
        String usedMappingRulesFsh = new SemanticMappingFshTraceRenderer().render(
                usedMappingRulesName,
                request.template().rootCdaType(),
                resolveMappingSourceDescription(request),
                conversion.usedMappingModel()
        );

        return new FhirConversionResult(
                List.of(new FshProfile(
                        conversion.profileName(),
                        conversion.fsh(),
                        request.template().id(),
                        request.template().rootCdaType(),
                        request.template().origin().name(),
                        "PROJECT".equals(request.template().origin().name()) ? "PROJECT" : "EXTERNAL",
                        "REQUIRED_INCLUDE".equals(request.template().origin().name()) ? "REQUIRED_INCLUDE" : "DIRECT",
                        false,
                        null,
                        null
                )),
                conversion.diagnostics(),
                mappingRulesName,
                mappingRulesFsh,
                usedMappingRulesName,
                usedMappingRulesFsh
        );
    }

    private String resolveMappingSourceDescription(FhirConversionRequest request) {
        if (request == null) {
            return "unknown";
        }
        if (request.structureMap() != null && !request.structureMap().isBlank()) {
            return "uploaded StructureMap override";
        }
        if (request.builtInMappingId() != null && !request.builtInMappingId().isBlank()) {
            return "built-in:" + request.builtInMappingId().trim();
        }
        return "built-in:default";
    }

    public SushiCompileResult compileWithSushi(SushiCompileRequest request) throws Exception {
        validateSushiRequest(request);
        String parent = firstNonBlank(request.parent(), extractParent(request.fshContent()));
        boolean baseObservationParent = isBaseObservationParent(parent);
        if (!baseObservationParent && (isBlank(request.dependencyPackageId()) || isBlank(request.dependencyVersion()))) {
            throw new BadRequestException("The official IG package id and version are required when Parent is not Observation");
        }

        Path tempDir = Files.createTempDirectory("axiom-fhir-sushi-");
        try {
            ParentProfileFshNormalizer.NormalizationResult normalized = new ParentProfileFshNormalizer().normalize(
                    request.fshContent(),
                    parent,
                    baseObservationParent,
                    request.dependencyPackageId(),
                    request.dependencyVersion()
            );

            Path fshDir = tempDir.resolve("input").resolve("fsh");
            Files.createDirectories(fshDir);
            Files.writeString(fshDir.resolve(safeFileName(request.profileName()) + ".fsh"), normalized.fshContent(), StandardCharsets.UTF_8);

            String sushiConfig = buildSushiConfig(request, baseObservationParent);
            Files.writeString(tempDir.resolve("sushi-config.yaml"), sushiConfig, StandardCharsets.UTF_8);

            ProcessResult processResult = runSushi(tempDir);
            List<String> diagnostics = diagnosticsFromOutput(processResult.output());
            diagnostics.addAll(0, normalized.diagnostics());
            if (processResult.exitCode() != 0) {
                diagnostics.add(0, "SUSHI failed with exit code " + processResult.exitCode() + ".");
                return new SushiCompileResult(false, null, diagnostics, sushiConfig, null);
            }

            Path generated = findGeneratedStructureDefinition(tempDir);
            if (generated == null) {
                diagnostics.add("SUSHI completed, but no generated StructureDefinition JSON was found.");
                return new SushiCompileResult(false, null, diagnostics, sushiConfig, null);
            }

            return new SushiCompileResult(
                    true,
                    Files.readString(generated, StandardCharsets.UTF_8),
                    diagnostics,
                    sushiConfig,
                    generated.getFileName().toString()
            );
        } catch (IOException e) {
            return new SushiCompileResult(
                    false,
                    null,
                    List.of("Unable to run SUSHI. Ensure the project SUSHI dependency is installed and available. " + e.getMessage()),
                    null,
                    null
            );
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private void validateSushiRequest(SushiCompileRequest request) {
        if (request == null) {
            throw new BadRequestException("SUSHI compile request is required");
        }
        if (isBlank(request.profileName())) {
            throw new BadRequestException("profileName is required");
        }
        if (isBlank(request.fshContent())) {
            throw new BadRequestException("fshContent is required");
        }
    }

    String buildSushiConfig(SushiCompileRequest request, boolean baseObservationParent) {
        StringBuilder builder = new StringBuilder();
        builder.append("id: axiom-cda-fhir-generated\n");
        builder.append("canonical: http://example.org/axiom-cda/fhir\n");
        builder.append("name: AxiomCdaFhirGenerated\n");
        builder.append("title: \"Axiom CDA FHIR Generated\"\n");
        builder.append("status: draft\n");
        builder.append("version: 0.1.0\n");
        builder.append("fhirVersion: ").append(R4_CORE_VERSION).append("\n");
        builder.append("FSHOnly: true\n");
        builder.append("dependencies:\n");
        for (Map.Entry<String, String> dependency : resolveSushiDependencies(request, baseObservationParent).entrySet()) {
            builder.append("  ").append(cleanYamlScalar(dependency.getKey())).append(": ")
                    .append(cleanYamlScalar(dependency.getValue())).append("\n");
        }
        return builder.toString();
    }

    Map<String, String> resolveSushiDependencies(SushiCompileRequest request, boolean baseObservationParent) {
        Map<String, String> dependencies = new LinkedHashMap<>();
        dependencies.put(R4_CORE_PACKAGE_ID, R4_CORE_VERSION);
        if (!baseObservationParent && !isBlank(request.dependencyPackageId()) && !isBlank(request.dependencyVersion())) {
            dependencies.put(request.dependencyPackageId().trim(), request.dependencyVersion().trim());
        }
        return dependencies;
    }

    private ProcessResult runSushi(Path projectDir) throws IOException {
        List<String> command = resolveSushiCommand();
        command.add(projectDir.toString());
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(projectDir.toFile());
        builder.redirectErrorStream(true);

        Process process = builder.start();
        StringBuilder output = new StringBuilder();
        Thread outputReader = new Thread(() -> {
            try {
                output.append(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                output.append("Unable to read SUSHI output: ").append(e.getMessage());
            }
        });
        outputReader.start();
        boolean completed;
        try {
            completed = process.waitFor(SUSHI_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new ProcessResult(1, "SUSHI execution was interrupted.");
        }
        if (!completed) {
            process.destroyForcibly();
            return new ProcessResult(1, "SUSHI timed out after " + SUSHI_TIMEOUT.toSeconds() + " seconds.");
        }
        try {
            outputReader.join(TimeUnit.SECONDS.toMillis(5));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new ProcessResult(process.exitValue(), output.toString());
    }

    private List<String> resolveSushiCommand() {
        String configuredCommand = System.getenv("SUSHI_COMMAND");
        if (!isBlank(configuredCommand)) {
            return new ArrayList<>(List.of(configuredCommand.trim().split("\\s+")));
        }

        List<Path> candidates = List.of(
                Path.of("axiom-cda-ws", "src", "main", "webui", "node_modules", ".bin", "sushi"),
                Path.of("src", "main", "webui", "node_modules", ".bin", "sushi"),
                Path.of("webui", "node_modules", ".bin", "sushi"),
                Path.of("/deployments", "webui", "node_modules", ".bin", "sushi")
        );
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return new ArrayList<>(List.of(candidate.toAbsolutePath().toString()));
            }
        }
        return new ArrayList<>(List.of("sushi"));
    }

    private List<String> diagnosticsFromOutput(String output) {
        if (isBlank(output)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Stream.of(output.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList());
    }

    private Path findGeneratedStructureDefinition(Path projectDir) throws IOException {
        Path resourcesDir = projectDir.resolve("fsh-generated").resolve("resources");
        if (!Files.isDirectory(resourcesDir)) {
            return null;
        }
        try (Stream<Path> files = Files.list(resourcesDir)) {
            return files
                    .filter(path -> path.getFileName().toString().startsWith("StructureDefinition-"))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .findFirst()
                    .orElse(null);
        }
    }

    private String extractParent(String fshContent) {
        if (fshContent == null) {
            return null;
        }
        for (String line : fshContent.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("Parent:")) {
                return trimmed.substring("Parent:".length()).trim();
            }
        }
        return null;
    }

    private boolean isBaseObservationParent(String parent) {
        return BASE_OBSERVATION_PARENT.equals(parent) || BASE_OBSERVATION_URL.equals(parent);
    }

    private String safeFileName(String profileName) {
        String sanitized = profileName.replaceAll("[^A-Za-z0-9._-]", "-");
        return sanitized.isBlank() ? "generated-observation-profile" : sanitized;
    }

    private String cleanYamlScalar(String value) {
        return value == null ? "" : value.replace("\n", "").replace("\r", "").trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted((left, right) -> right.compareTo(left))
                    .forEach(item -> {
                        try {
                            Files.deleteIfExists(item);
                        } catch (IOException ignored) {
                            // Best-effort cleanup for temporary SUSHI projects.
                        }
                    });
        } catch (IOException ignored) {
            // Best-effort cleanup for temporary SUSHI projects.
        }
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
