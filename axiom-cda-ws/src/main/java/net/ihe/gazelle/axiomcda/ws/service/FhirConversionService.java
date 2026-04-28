package net.ihe.gazelle.axiomcda.ws.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import net.ihe.gazelle.axiomcda.engine.business.ObservationFhirConversionResult;
import net.ihe.gazelle.axiomcda.engine.business.ObservationIrToFhirFshGenerator;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@ApplicationScoped
public class FhirConversionService {

    private static final Duration SUSHI_TIMEOUT = Duration.ofSeconds(120);
    private static final String BASE_OBSERVATION_PARENT = "Observation";
    private static final String BASE_OBSERVATION_URL = "http://hl7.org/fhir/StructureDefinition/Observation";

    public FhirConversionResult convertObservation(FhirConversionRequest request) throws Exception {
        if (request == null || request.template() == null) {
            throw new IllegalArgumentException("A selected IR template is required for FHIR conversion");
        }
        if (request.structureMap() == null || request.structureMap().isBlank()) {
            throw new IllegalArgumentException("A StructureMap JSON upload is required for FHIR conversion");
        }

        ObservationIrToFhirFshGenerator generator = new ObservationIrToFhirFshGenerator();
        ObservationFhirConversionResult conversion = generator.generate(
                request.template(),
                request.sourceProfileName(),
                request.structureMap()
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
                conversion.diagnostics()
        );
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

    private String buildSushiConfig(SushiCompileRequest request, boolean baseObservationParent) {
        StringBuilder builder = new StringBuilder();
        builder.append("id: axiom-cda-fhir-generated\n");
        builder.append("canonical: http://example.org/axiom-cda/fhir\n");
        builder.append("name: AxiomCdaFhirGenerated\n");
        builder.append("title: \"Axiom CDA FHIR Generated\"\n");
        builder.append("status: draft\n");
        builder.append("version: 0.1.0\n");
        builder.append("fhirVersion: 4.0.1\n");
        builder.append("FSHOnly: true\n");
        builder.append("dependencies:\n");
        builder.append("  hl7.fhir.r4.core: 4.0.1\n");
        if (!baseObservationParent) {
            builder.append("  ").append(cleanYamlScalar(request.dependencyPackageId())).append(": ")
                    .append(cleanYamlScalar(request.dependencyVersion())).append("\n");
        }
        return builder.toString();
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
