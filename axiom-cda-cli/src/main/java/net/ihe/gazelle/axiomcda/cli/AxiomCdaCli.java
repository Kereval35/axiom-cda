package net.ihe.gazelle.axiomcda.cli;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ihe.gazelle.axiomcda.api.bbr.BusinessNameWithLanguage;
import net.ihe.gazelle.axiomcda.api.bbr.Decor;
import net.ihe.gazelle.axiomcda.api.bbr.Project;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.config.TemplateSelection;
import net.ihe.gazelle.axiomcda.api.fsh.FshBundle;
import net.ihe.gazelle.axiomcda.api.ir.IRDiagnostic;
import net.ihe.gazelle.axiomcda.api.ir.IRDiagnosticSeverity;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.api.ir.IrTransformResult;
import net.ihe.gazelle.axiomcda.api.port.BbrLoader;
import net.ihe.gazelle.axiomcda.api.port.BbrToIrTransformer;
import net.ihe.gazelle.axiomcda.api.port.CdaModelRepository;
import net.ihe.gazelle.axiomcda.api.port.FshWriter;
import net.ihe.gazelle.axiomcda.api.port.GenerationReportWriter;
import net.ihe.gazelle.axiomcda.api.port.IrToFshGenerator;
import net.ihe.gazelle.axiomcda.api.report.GenerationReport;
import net.ihe.gazelle.axiomcda.engine.business.DefaultBbrToIrTransformer;
import net.ihe.gazelle.axiomcda.engine.business.DefaultIrToFshGenerator;
import net.ihe.gazelle.axiomcda.engine.business.DefaultTerminologyToFshGenerator;
import net.ihe.gazelle.axiomcda.engine.technical.ConsoleGenerationReportWriter;
import net.ihe.gazelle.axiomcda.engine.technical.DefaultFshWriter;
import net.ihe.gazelle.axiomcda.engine.technical.JaxbBbrLoader;
import net.ihe.gazelle.axiomcda.engine.technical.JsonCdaModelRepository;
import net.ihe.gazelle.axiomcda.engine.technical.YamlConfigLoader;
import net.ihe.gazelle.axiomcda.engine.util.ResourcePaths;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(
        name = "axiom-cda",
        description = "Generate FSH-CDA profiles and terminology from ART-DECOR BBR exports.",
        mixinStandardHelpOptions = true,
        subcommands = {AxiomCdaCli.GenerateCommand.class}
)
public class AxiomCdaCli implements Runnable {
    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new AxiomCdaCli());
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    @Command(
            name = "generate",
            description = "Generate FSH-CDA profiles, invariants, and terminology from a BBR export.",
            sortOptions = false
    )
    static class GenerateCommand implements Callable<Integer> {
        @Option(names = "--bbr", required = true, paramLabel = "<path>", description = "Path to the BBR XML (required).")
        private Path bbrPath;

        @Option(names = "--out", required = true, paramLabel = "<dir>", description = "Output directory (required).")
        private Path outputDir;

        @Option(names = "--cda-package", paramLabel = "<dir>", description = "Override CDA package directory.")
        private Path cdaPackage;

        @Option(names = "--ans-reference", paramLabel = "<dir>", description = "Optional ANS FSH path (comparison only).")
        private Path ansReference;

        @Option(names = "--config", paramLabel = "<yaml>", description = "Generation config override.")
        private Path configPath;

        @Option(names = "--profile-prefix", paramLabel = "<s>", description = "Prefix profile names.")
        private String profilePrefix;

        @Option(names = "--id-prefix", paramLabel = "<s>", description = "Prefix profile ids.")
        private String idPrefix;

        @Option(names = "--title-prefix", paramLabel = "<s>", description = "Prefix profile titles.")
        private String titlePrefix;

        @Option(names = "--resources-dir", paramLabel = "<s>", description = "Output folder for profiles.")
        private String resourcesDir;

        @Option(names = "--invariants-dir", paramLabel = "<s>", description = "Output folder for invariants.")
        private String invariantsDir;

        @Option(names = "--classification-types", split = ",", paramLabel = "<csv>",
                description = "Template classification filters (CSV).")
        private List<String> classificationTypes;

        @Option(names = "--template-ids", split = ",", paramLabel = "<csv>",
                description = "Explicit template ids to generate (CSV).")
        private List<String> templateIds;

        @Option(names = "--all-templates", description = "Ignore classification filters and generate all.")
        private boolean allTemplates;

        @Option(names = "--sushi-repo", description = "Emit a SUSHI-ready repo (sushi-config.yaml + input/fsh).")
        private boolean sushiRepo;

        @Option(names = "--ig-id", paramLabel = "<s>", description = "IG id for sushi-config.yaml.")
        private String igId;

        @Option(names = "--ig-name", paramLabel = "<s>", description = "IG name for sushi-config.yaml.")
        private String igName;

        @Option(names = "--ig-title", paramLabel = "<s>", description = "IG title for sushi-config.yaml.")
        private String igTitle;

        @Option(names = "--ig-canonical", paramLabel = "<url>", description = "IG canonical for sushi-config.yaml.")
        private String igCanonical;

        @Option(names = "--ig-version", paramLabel = "<s>", description = "IG version for sushi-config.yaml.")
        private String igVersion;

        @Option(names = "--ig-copyright-year", paramLabel = "<s>",
                description = "IG copyrightYear for sushi-config.yaml.")
        private String igCopyrightYear;

        @Option(names = "--ig-release-label", paramLabel = "<s>",
                description = "IG releaseLabel for sushi-config.yaml.")
        private String igReleaseLabel;

        @Option(names = "--emit-ir", description = "Emit the intermediate representation as axiom-cda-ir.json.")
        private Boolean emitIrSnapshot;

        @Override
        public Integer call() {
            if (bbrPath != null) {
                if (!Files.exists(bbrPath)) {
                    System.err.println("BBR path not found: " + bbrPath);
                    return 2;
                }
                if (Files.isDirectory(bbrPath)) {
                    System.err.println("BBR path must be a file: " + bbrPath);
                    return 2;
                }
            }
            if (outputDir != null && Files.exists(outputDir) && !Files.isDirectory(outputDir)) {
                System.err.println("Output path must be a directory: " + outputDir);
                return 2;
            }
            if (ansReference != null && !Files.exists(ansReference)) {
                System.err.println("ANS reference path not found: " + ansReference);
            }

            try {
                GenerationConfig config = GenerationConfig.defaults();
                if (configPath != null) {
                    config = new YamlConfigLoader().load(configPath);
                }
                if (profilePrefix != null || idPrefix != null || titlePrefix != null) {
                    config = applyNamingOverrides(config, profilePrefix, idPrefix, titlePrefix);
                }
                if (allTemplates || classificationTypes != null || templateIds != null) {
                    config = applySelectionOverrides(config, classificationTypes, templateIds, allTemplates);
                }
                if (emitIrSnapshot != null) {
                    config = applyIrOverride(config, emitIrSnapshot);
                }

                Path packagePath = resolvePackagePath(cdaPackage);
                CdaModelRepository cdaRepository = new JsonCdaModelRepository(packagePath);
                BbrLoader bbrLoader = new JaxbBbrLoader();
                Decor decor = bbrLoader.load(bbrPath);
                String resolvedResourcesDir = resourcesDir != null ? resourcesDir : deriveResourcesDir(decor);
                String resolvedInvariantsDir = invariantsDir;

                BbrToIrTransformer transformer = new DefaultBbrToIrTransformer();
                IrTransformResult transformResult = transformer.transform(decor, config, cdaRepository);

                IrToFshGenerator generator = new DefaultIrToFshGenerator();
                FshBundle bundle = generator.generate(transformResult.templates(), config, cdaRepository);
                DefaultTerminologyToFshGenerator terminologyGenerator = new DefaultTerminologyToFshGenerator();
                FshBundle terminologyBundle = terminologyGenerator.generate(decor, config);
                bundle = mergeBundles(bundle, terminologyBundle);
                bundle = remapBundle(bundle, resolvedResourcesDir, resolvedInvariantsDir);

                FshWriter writer = new DefaultFshWriter();
                Path repoRoot = outputDir;
                Path fshOutputDir = sushiRepo ? repoRoot.resolve("input").resolve("fsh") : repoRoot;
                writer.write(fshOutputDir, bundle);
                if (sushiRepo) {
                    writeSushiConfig(repoRoot, packagePath, this);
                }

                if (config.emitIrSnapshot()) {
                    writeIrSnapshot(repoRoot, transformResult);
                }

                GenerationReport report = buildReport(transformResult, bundle);
                GenerationReportWriter reportWriter = new ConsoleGenerationReportWriter();
                reportWriter.write(report);
                try {
                    writeJsonReport(repoRoot, report);
                } catch (Exception e) {
                    System.err.println("Failed to write JSON report: " + e.getMessage());
                }

                System.out.println("Output directory: " + repoRoot.toAbsolutePath());
                boolean hasErrors = transformResult.diagnostics().stream()
                        .anyMatch(diag -> diag.severity() == IRDiagnosticSeverity.ERROR);
                return hasErrors ? 2 : 0;
            } catch (Exception e) {
                System.err.println("Generation failed: " + e.getMessage());
                return 3;
            }
        }
    }

    private static Path resolvePackagePath(Path cdaPackage) {
        if (cdaPackage == null) {
            return ResourcePaths.getResourcePath("package");
        }
        if (Files.isRegularFile(cdaPackage)
                && cdaPackage.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".tgz")) {
            throw new IllegalArgumentException("CDA package .tgz not supported; please provide extracted directory");
        }
        return cdaPackage;
    }

    private static GenerationReport buildReport(IrTransformResult transformResult, FshBundle bundle) {
        int profiles = 0;
        int invariants = 0;
        for (String fsh : bundle.files().values()) {
            if (fsh.startsWith("Invariant:")) {
                invariants++;
            } else if (fsh.startsWith("Profile:")) {
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
            if (lower.contains("unmapped")) {
                unmapped++;
            }
            if (lower.contains("valueset")) {
                unresolved++;
            }
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

    private static void writeIrSnapshot(Path outputDir, IrTransformResult transformResult) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Path target = outputDir.resolve("axiom-cda-ir.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), transformResult.templates());
    }

    private static void writeJsonReport(Path outputDir, GenerationReport report) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Path target = outputDir.resolve("axiom-cda-report.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), report);
    }

    private static void writeSushiConfig(Path outputDir, Path packagePath, GenerateCommand options) throws Exception {
        CdaPackageInfo packageInfo = readCdaPackageInfo(packagePath);
        String igId = options.igId != null ? options.igId : "axiom-cda-generated";
        String igName = options.igName != null ? options.igName : "AxiomCdaGenerated";
        String igTitle = options.igTitle != null ? options.igTitle : "Axiom CDA Generated";
        String igCanonical = options.igCanonical != null ? options.igCanonical : "http://example.org/axiom-cda";
        String igVersion = options.igVersion != null ? options.igVersion : "0.1.0";
        String igCopyrightYear = options.igCopyrightYear != null
                ? options.igCopyrightYear
                : String.valueOf(Year.now().getValue());
        String igReleaseLabel = options.igReleaseLabel != null ? options.igReleaseLabel : "draft";
        String fhirVersion = packageInfo.fhirVersions != null && !packageInfo.fhirVersions.isEmpty()
                ? packageInfo.fhirVersions.getFirst()
                : "5.0.0";

        Map<String, String> dependencies = new LinkedHashMap<>();
        if (packageInfo.name != null && packageInfo.version != null) {
            dependencies.put(packageInfo.name, packageInfo.version);
        }
        if (packageInfo.dependencies != null) {
            for (Map.Entry<String, String> entry : packageInfo.dependencies.entrySet()) {
                dependencies.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("id: ").append(igId).append("\n");
        builder.append("canonical: ").append(igCanonical).append("\n");
        builder.append("name: ").append(igName).append("\n");
        builder.append("title: \"").append(escapeYaml(igTitle)).append("\"\n");
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

    private static String escapeYaml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\\\"");
    }

    private static CdaPackageInfo readCdaPackageInfo(Path packagePath) throws Exception {
        Path packageJson = packagePath.resolve("package.json");
        if (!Files.isRegularFile(packageJson)) {
            return new CdaPackageInfo(null, null, null, null, null);
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(packageJson.toFile(), CdaPackageInfo.class);
    }

    private static String deriveResourcesDir(Decor decor) {
        String base = DefaultIrToFshGenerator.RESOURCES_DIR;
        String token = deriveProjectToken(decor);
        if (token.isBlank()) {
            return base;
        }
        if (token.startsWith(base)) {
            return token;
        }
        return base + token;
    }

    private static String deriveProjectToken(Decor decor) {
        if (decor == null || decor.getProject() == null) {
            return "";
        }
        Project project = decor.getProject();
        String name = selectProjectName(project, decor.getLanguage());
        if (name == null || name.isBlank()) {
            name = project.getPrefix();
        }
        return normalizeToken(name);
    }

    private static String selectProjectName(Project project, String language) {
        if (project == null) {
            return null;
        }
        List<BusinessNameWithLanguage> names = project.getName();
        if (names == null || names.isEmpty()) {
            return project.getPrefix();
        }
        if (language != null) {
            for (BusinessNameWithLanguage name : names) {
                if (name == null || name.getLanguage() == null) {
                    continue;
                }
                if (sameLanguage(language, name.getLanguage())) {
                    return name.getValue();
                }
            }
        }
        for (BusinessNameWithLanguage name : names) {
            if (name != null && name.getValue() != null && !name.getValue().isBlank()) {
                return name.getValue();
            }
        }
        return project.getPrefix();
    }

    private static boolean sameLanguage(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        String expectedLower = expected.toLowerCase(Locale.ROOT);
        String actualLower = actual.toLowerCase(Locale.ROOT);
        return expectedLower.equals(actualLower)
                || expectedLower.startsWith(actualLower)
                || actualLower.startsWith(expectedLower);
    }

    private static String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        StringBuilder builder = new StringBuilder();
        boolean uppercaseNext = true;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.getType(c) == Character.NON_SPACING_MARK) {
                continue;
            }
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                if (uppercaseNext && c >= 'a' && c <= 'z') {
                    c = (char) (c - ('a' - 'A'));
                }
                builder.append(c);
                uppercaseNext = false;
            } else {
                uppercaseNext = true;
            }
        }
        return builder.toString();
    }

    private static GenerationConfig applyNamingOverrides(GenerationConfig config,
                                                         String profilePrefix,
                                                         String idPrefix,
                                                         String titlePrefix) {
        var existing = config.naming();
        String resolvedProfilePrefix = profilePrefix != null ? profilePrefix : existing.profilePrefix();
        String resolvedIdPrefix = idPrefix != null ? idPrefix : existing.idPrefix();
        String resolvedTitlePrefix = titlePrefix != null ? titlePrefix : existing.titlePrefix();
        var naming = new net.ihe.gazelle.axiomcda.api.config.NamingConfig(
                resolvedProfilePrefix,
                resolvedIdPrefix,
                resolvedTitlePrefix,
                existing.profileNameOverrides(),
                existing.idOverrides()
        );
        return new GenerationConfig(naming, config.nullFlavorPolicy(), config.valueSetPolicy(),
                config.templateSelection(), config.emitInvariants(), config.emitIrSnapshot());
    }

    private static GenerationConfig applySelectionOverrides(GenerationConfig config,
                                                            List<String> classificationTypes,
                                                            List<String> templateIds,
                                                            boolean allTemplates) {
        TemplateSelection existing = config.templateSelection();
        List<String> resolvedTemplateIds = templateIds != null ? templateIds : existing.templateIds();
        List<String> resolvedClassificationTypes = classificationTypes != null
                ? classificationTypes
                : existing.classificationTypes();
        if (allTemplates) {
            resolvedTemplateIds = List.of();
            resolvedClassificationTypes = List.of();
        }
        TemplateSelection selection = new TemplateSelection(resolvedClassificationTypes, resolvedTemplateIds);
        return new GenerationConfig(config.naming(), config.nullFlavorPolicy(), config.valueSetPolicy(),
                selection, config.emitInvariants(), config.emitIrSnapshot());
    }

    private static GenerationConfig applyIrOverride(GenerationConfig config, boolean emitIrSnapshot) {
        return new GenerationConfig(config.naming(), config.nullFlavorPolicy(), config.valueSetPolicy(),
                config.templateSelection(), config.emitInvariants(), emitIrSnapshot);
    }

    private static FshBundle remapBundle(FshBundle bundle, String resourcesDir, String invariantsDir) {
        String baseResourcesDir = DefaultIrToFshGenerator.RESOURCES_DIR;
        String baseInvariantsDir = "Invariants";
        String targetResourcesDir = (resourcesDir == null || resourcesDir.isBlank()) ? baseResourcesDir : resourcesDir;
        String targetInvariantsDir = (invariantsDir == null || invariantsDir.isBlank()) ? baseInvariantsDir : invariantsDir;
        String legacyResourcesDir = "ResourcesCDAEntete";
        boolean hasLegacyResources = bundle.files().keySet().stream()
                .anyMatch(path -> path.startsWith(legacyResourcesDir + "/"));
        boolean needsRemap = hasLegacyResources
                || !targetResourcesDir.equals(baseResourcesDir)
                || !targetInvariantsDir.equals(baseInvariantsDir);
        if (!needsRemap) {
            return bundle;
        }
        Map<String, String> remapped = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : bundle.files().entrySet()) {
            String path = entry.getKey();
            if (path.startsWith(baseResourcesDir + "/")) {
                path = targetResourcesDir + "/" + path.substring(baseResourcesDir.length() + 1);
            } else if (path.startsWith(legacyResourcesDir + "/")) {
                path = targetResourcesDir + "/" + path.substring(legacyResourcesDir.length() + 1);
            } else if (path.startsWith(baseInvariantsDir + "/")) {
                path = targetInvariantsDir + "/" + path.substring(baseInvariantsDir.length() + 1);
            }
            remapped.put(path, entry.getValue());
        }
        return new FshBundle(remapped);
    }

    private static FshBundle mergeBundles(FshBundle primary, FshBundle secondary) {
        if (secondary == null || secondary.files().isEmpty()) {
            return primary;
        }
        Map<String, String> merged = new LinkedHashMap<>();
        merged.putAll(primary.files());
        for (Map.Entry<String, String> entry : secondary.files().entrySet()) {
            String path = entry.getKey();
            String content = entry.getValue();
            if (!merged.containsKey(path)) {
                merged.put(path, content);
                continue;
            }
            String adjusted = uniquifyPath(path, merged);
            merged.put(adjusted, content);
        }
        return new FshBundle(merged);
    }

    private static String uniquifyPath(String path, Map<String, String> existing) {
        String base = path;
        String suffix = "";
        int dot = path.lastIndexOf('.');
        if (dot > 0) {
            base = path.substring(0, dot);
            suffix = path.substring(dot);
        }
        int counter = 2;
        String candidate = base + "-" + counter + suffix;
        while (existing.containsKey(candidate)) {
            counter++;
            candidate = base + "-" + counter + suffix;
        }
        return candidate;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CdaPackageInfo(String name,
                                  String version,
                                  String canonical,
                                  List<String> fhirVersions,
                                  Map<String, String> dependencies) {
    }
}
