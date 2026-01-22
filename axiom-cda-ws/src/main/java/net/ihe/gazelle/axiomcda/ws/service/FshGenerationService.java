package net.ihe.gazelle.axiomcda.ws.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import net.ihe.gazelle.axiomcda.api.bbr.Decor;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.fsh.FshBundle;
import net.ihe.gazelle.axiomcda.api.ir.IRDiagnostic;
import net.ihe.gazelle.axiomcda.api.ir.IRDiagnosticSeverity;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.api.ir.IrTransformResult;
import net.ihe.gazelle.axiomcda.api.port.BbrLoader;
import net.ihe.gazelle.axiomcda.api.port.CdaModelRepository;
import net.ihe.gazelle.axiomcda.api.port.FshWriter;
import net.ihe.gazelle.axiomcda.api.port.IrToFshGenerator;
import net.ihe.gazelle.axiomcda.api.port.BbrToIrTransformer;
import net.ihe.gazelle.axiomcda.api.report.GenerationReport;
import net.ihe.gazelle.axiomcda.engine.business.DefaultBbrToIrTransformer;
import net.ihe.gazelle.axiomcda.engine.business.DefaultIrToFshGenerator;
import net.ihe.gazelle.axiomcda.engine.business.DefaultTerminologyToFshGenerator;
import net.ihe.gazelle.axiomcda.engine.technical.DefaultFshWriter;
import net.ihe.gazelle.axiomcda.engine.technical.JaxbBbrLoader;
import net.ihe.gazelle.axiomcda.engine.technical.JsonCdaModelRepository;
import net.ihe.gazelle.axiomcda.engine.technical.YamlConfigLoader;
import net.ihe.gazelle.axiomcda.engine.util.ResourcePaths;
import net.ihe.gazelle.axiomcda.ws.dto.FshProfile;
import net.ihe.gazelle.axiomcda.ws.dto.GenerationRequest;
import net.ihe.gazelle.axiomcda.ws.dto.GenerationResult;
import net.ihe.gazelle.axiomcda.ws.util.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class FshGenerationService {

    private static final Logger LOG = LoggerFactory.getLogger(FshGenerationService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public GenerationResult generateFsh(GenerationRequest request) throws Exception {
        Path tempDir = Files.createTempDirectory("axiom-cda-gen-" + UUID.randomUUID());
        try {
            Path bbrPath = resolveBbrPath(request.bbr(), tempDir);
            GenerationConfig config = resolveConfig(request.yamlConfig());
            
            config = new GenerationConfig(
                    config.naming(),
                    config.nullFlavorPolicy(),
                    config.valueSetPolicy(),
                    config.templateSelection(),
                    config.emitInvariants(),
                    request.emitIr()
            );

            Path packagePath = ResourcePaths.getResourcePath("package");
            CdaModelRepository cdaRepository = new JsonCdaModelRepository(packagePath);
            BbrLoader bbrLoader = new JaxbBbrLoader();
            Decor decor = bbrLoader.load(bbrPath);

            BbrToIrTransformer transformer = new DefaultBbrToIrTransformer();
            IrTransformResult transformResult = transformer.transform(decor, config, cdaRepository);

            IrToFshGenerator generator = new DefaultIrToFshGenerator();
            FshBundle bundle = generator.generate(transformResult.templates(), config, cdaRepository);
            
            DefaultTerminologyToFshGenerator terminologyGenerator = new DefaultTerminologyToFshGenerator();
            FshBundle terminologyBundle = terminologyGenerator.generate(decor, config);
            
            bundle.files().putAll(terminologyBundle.files());

            FshWriter writer = new DefaultFshWriter();
            Path outDir = tempDir.resolve("output");
            Files.createDirectories(outDir);
            
            Path fshOutputDir = request.sushiRepo() ? outDir.resolve("input").resolve("fsh") : outDir;
            if (request.sushiRepo()) {
                Files.createDirectories(fshOutputDir);
                writeSushiConfig(outDir, packagePath);
            }
            
            writer.write(fshOutputDir, bundle);

            if (config.emitIrSnapshot()) {
                writeIrSnapshot(outDir, transformResult);
            }

            GenerationReport report = buildReport(transformResult, bundle);

            List<FshProfile> profiles = extractProfiles(bundle);

            Path zipPath = tempDir.resolve("axiom-cda-fsh.zip");
            ZipUtils.zipDirectory(outDir, zipPath);

            byte[] zipBytes = Files.readAllBytes(zipPath);
            String zipBase64 = Base64.getEncoder().encodeToString(zipBytes);

            List<IRTemplate> irTemplates = config.emitIrSnapshot() ? transformResult.templates() : List.of();

            return new GenerationResult(zipBase64, report, profiles, irTemplates);
        } catch (Exception e) {
            LOG.error("Generation failed", e);
            throw e;
        }
    }

    private Path resolveBbrPath(String bbr, Path tempDir) throws IOException {
        if (bbr.startsWith("http://") || bbr.startsWith("https://")) {
            LOG.info("Fetching BBR from URL: {}", bbr);
            Path bbrFile = tempDir.resolve("bbr.xml");
            try (InputStream in = URI.create(bbr).toURL().openStream()) {
                Files.copy(in, bbrFile);
            }
            return bbrFile;
        } else {
            LOG.info("Assuming BBR is provided as XML content");
            Path bbrFile = tempDir.resolve("bbr.xml");
            Files.writeString(bbrFile, bbr);
            return bbrFile;
        }
    }

    private GenerationConfig resolveConfig(String yamlConfig) {
        if (yamlConfig == null || yamlConfig.isBlank()) {
            return GenerationConfig.defaults();
        }
        try {
            Path tempConfig = Files.createTempFile("config", ".yaml");
            Files.writeString(tempConfig, yamlConfig);
            return new YamlConfigLoader().load(tempConfig);
        } catch (IOException e) {
            LOG.warn("Failed to load provided YAML config, using defaults", e);
            return GenerationConfig.defaults();
        }
    }

    private void writeSushiConfig(Path outputDir, Path packagePath) throws Exception {
        CdaPackageInfo packageInfo = readCdaPackageInfo(packagePath);
        String igId = "axiom-cda-generated";
        String igName = "AxiomCdaGenerated";
        String igTitle = "Axiom CDA Generated";
        String igCanonical = "http://example.org/axiom-cda";
        String igVersion = "0.1.0";
        String igCopyrightYear = String.valueOf(Year.now().getValue());
        String igReleaseLabel = "draft";
        String fhirVersion = packageInfo.fhirVersions != null && !packageInfo.fhirVersions.isEmpty()
                ? packageInfo.fhirVersions.get(0)
                : "5.0.0";

        Map<String, String> dependencies = new LinkedHashMap<>();
        if (packageInfo.name != null && packageInfo.version != null) {
            dependencies.put(packageInfo.name, packageInfo.version);
        }
        if (packageInfo.dependencies != null) {
            dependencies.putAll(packageInfo.dependencies);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("id: ").append(igId).append("\n");
        builder.append("canonical: ").append(igCanonical).append("\n");
        builder.append("name: ").append(igName).append("\n");
        builder.append("title: \"").append(igTitle.replace("\"", "\\\"")).append("\"\n");
        builder.append("status: draft\n");
        builder.append("version: ").append(igVersion).append("\n");
        builder.append("copyrightYear: ").append(igCopyrightYear).append("\n");
        builder.append("releaseLabel: ").append(igReleaseLabel).append("\n");
        builder.append("fhirVersion: ").append(fhirVersion).append("\n");
        if (!dependencies.isEmpty()) {
            builder.append("dependencies:\n");
            for (Map.Entry<String, String> entry : dependencies.entrySet()) {
                builder.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        Path target = outputDir.resolve("sushi-config.yaml");
        Files.writeString(target, builder.toString(), StandardCharsets.UTF_8);
    }

    private CdaPackageInfo readCdaPackageInfo(Path packagePath) throws Exception {
        Path packageJson = packagePath.resolve("package.json");
        if (!Files.isRegularFile(packageJson)) {
            return new CdaPackageInfo(null, null, null, null, null);
        }
        return MAPPER.readValue(packageJson.toFile(), CdaPackageInfo.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CdaPackageInfo(String name,
                                  String version,
                                  String canonical,
                                  List<String> fhirVersions,
                                  Map<String, String> dependencies) {
    }

    private GenerationReport buildReport(IrTransformResult transformResult, FshBundle bundle) {
        int profiles = 0;
        int invariants = 0;
        for (String fsh : bundle.files().values()) {
            if (fsh.trim().startsWith("Invariant:")) {
                invariants++;
            } else if (fsh.trim().startsWith("Profile:")) {
                profiles++;
            }
        }
        int unmapped = 0;
        int unresolved = 0;
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> templatesWithIssues = new HashSet<>();
        for (IRDiagnostic diagnostic : transformResult.diagnostics()) {
            String lower = diagnostic.message().toLowerCase(Locale.ROOT);
            if (lower.contains("unmapped")) unmapped++;
            if (lower.contains("valueset")) unresolved++;
            
            if (diagnostic.severity() == IRDiagnosticSeverity.WARNING
                    || diagnostic.severity() == IRDiagnosticSeverity.ERROR) {
                StringBuilder builder = new StringBuilder();
                builder.append(diagnostic.severity()).append(": ");
                if (diagnostic.templateId() != null) {
                    builder.append("[").append(diagnostic.templateId()).append("] ");
                    templatesWithIssues.add(diagnostic.templateId());
                }
                if (diagnostic.path() != null) {
                    builder.append(diagnostic.path()).append(" - ");
                }
                builder.append(diagnostic.message());
                if (diagnostic.severity() == IRDiagnosticSeverity.ERROR) {
                    errors.add(builder.toString());
                } else {
                    warnings.add(builder.toString());
                }
            }
        }
        int templatesGenerated = transformResult.templates().size();
        int templatesConsidered = transformResult.templatesConsidered();
        int templatesSkipped = Math.max(0, templatesConsidered - templatesGenerated);
        int templatesOk = 0;
        for (IRTemplate template : transformResult.templates()) {
            String templateId = template.id();
            if (templateId == null || !templatesWithIssues.contains(templateId)) {
                templatesOk++;
            }
        }
        return new GenerationReport(
                templatesConsidered,
                templatesGenerated,
                templatesSkipped,
                templatesOk,
                profiles,
                invariants,
                unmapped,
                unresolved,
                warnings,
                errors
        );
    }

    private List<FshProfile> extractProfiles(FshBundle bundle) {
        List<FshProfile> profiles = new ArrayList<>();
        String resourcesDir = DefaultIrToFshGenerator.RESOURCES_DIR + "/";
        for (var entry : bundle.files().entrySet()) {
            String filePath = entry.getKey();
            if (filePath.startsWith(resourcesDir)) {
                String fileName = filePath.substring(resourcesDir.length());
                String profileName = fileName.endsWith(".fsh") ? fileName.substring(0, fileName.length() - 4) : fileName;
                profiles.add(new FshProfile(profileName, entry.getValue()));
            }
        }
        return profiles;
    }

    private void writeIrSnapshot(Path outputDir, IrTransformResult transformResult) throws Exception {
        Path target = outputDir.resolve("axiom-cda-ir.json");
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), transformResult.templates());
        LOG.info("IR snapshot written to {}", target);
    }
}
