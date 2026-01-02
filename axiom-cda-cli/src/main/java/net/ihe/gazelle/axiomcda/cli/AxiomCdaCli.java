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
import net.ihe.gazelle.axiomcda.api.ir.IrTransformResult;
import net.ihe.gazelle.axiomcda.api.port.*;
import net.ihe.gazelle.axiomcda.api.report.GenerationReport;
import net.ihe.gazelle.axiomcda.engine.business.DefaultBbrToIrTransformer;
import net.ihe.gazelle.axiomcda.engine.business.DefaultIrToFshGenerator;
import net.ihe.gazelle.axiomcda.engine.technical.*;
import net.ihe.gazelle.axiomcda.engine.util.ResourcePaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AxiomCdaCli {
    public static void main(String[] args) {
        if (args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
            printUsage();
            return;
        }
        if (!"generate".equals(args[0])) {
            System.err.println("Unknown command: " + args[0]);
            printUsage();
            System.exit(1);
            return;
        }

        CliOptions options = parseOptions(args);
        if (options == null) {
            System.exit(1);
            return;
        }

        try {
            GenerationConfig config = GenerationConfig.defaults();
            if (options.configPath != null) {
                config = new YamlConfigLoader().load(options.configPath);
            }
            if (options.profilePrefix != null || options.idPrefix != null || options.titlePrefix != null) {
                config = applyNamingOverrides(config, options);
            }
            if (options.allTemplates || options.classificationTypes != null || options.templateIds != null) {
                config = applySelectionOverrides(config, options);
            }

            Path packagePath = resolvePackagePath(options.cdaPackage);
            CdaModelRepository cdaRepository = new JsonCdaModelRepository(packagePath);
            BbrLoader bbrLoader = new JaxbBbrLoader();
            Decor decor = bbrLoader.load(options.bbrPath);
            String resourcesDir = options.resourcesDir != null ? options.resourcesDir : deriveResourcesDir(decor);
            String invariantsDir = options.invariantsDir;

            BbrToIrTransformer transformer = new DefaultBbrToIrTransformer();
            IrTransformResult transformResult = transformer.transform(decor, config, cdaRepository);

            IrToFshGenerator generator = new DefaultIrToFshGenerator();
            FshBundle bundle = generator.generate(transformResult.templates(), config, cdaRepository);
            bundle = remapBundle(bundle, resourcesDir, invariantsDir);

            FshWriter writer = new DefaultFshWriter();
            Path repoRoot = options.outputDir;
            Path fshOutputDir = options.sushiRepo ? repoRoot.resolve("input").resolve("fsh") : repoRoot;
            writer.write(fshOutputDir, bundle);
            if (options.sushiRepo) {
                writeSushiConfig(repoRoot, packagePath, options);
            }

            if (config.emitIrSnapshot()) {
                writeIrSnapshot(repoRoot, transformResult);
            }

            GenerationReport report = buildReport(transformResult, bundle);
            GenerationReportWriter reportWriter = new ConsoleGenerationReportWriter();
            reportWriter.write(report);

            System.out.println("Output directory: " + repoRoot.toAbsolutePath());
            int exitCode = transformResult.diagnostics().stream().anyMatch(diag -> diag.severity() == IRDiagnosticSeverity.ERROR) ? 2 : 0;
            System.exit(exitCode);
        } catch (Exception e) {
            System.err.println("Generation failed: " + e.getMessage());
            System.exit(3);
        }
    }

    private static Path resolvePackagePath(Path cdaPackage) {
        if (cdaPackage == null) {
            return ResourcePaths.getResourcePath("package");
        }
        if (Files.isRegularFile(cdaPackage) && cdaPackage.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".tgz")) {
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
        for (IRDiagnostic diagnostic : transformResult.diagnostics()) {
            String lower = diagnostic.message().toLowerCase(Locale.ROOT);
            if (lower.contains("unmapped")) {
                unmapped++;
            }
            if (lower.contains("valueset")) {
                unresolved++;
            }
            if (diagnostic.severity() != IRDiagnosticSeverity.INFO) {
                StringBuilder builder = new StringBuilder();
                builder.append(diagnostic.severity()).append(": ");
                if (diagnostic.templateId() != null) {
                    builder.append("[").append(diagnostic.templateId()).append("] ");
                }
                if (diagnostic.path() != null) {
                    builder.append(diagnostic.path()).append(" - ");
                }
                builder.append(diagnostic.message());
                warnings.add(builder.toString());
            }
        }
        return new GenerationReport(
                transformResult.templates().size(),
                profiles,
                invariants,
                unmapped,
                unresolved,
                warnings
        );
    }

    private static void writeIrSnapshot(Path outputDir, IrTransformResult transformResult) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Path target = outputDir.resolve("ir.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), transformResult.templates());
    }

    private static void writeSushiConfig(Path outputDir, Path packagePath, CliOptions options) throws Exception {
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

    private static List<String> parseCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
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

    private static String selectProjectName(Project project, String decorLanguage) {
        if (project == null || project.getName() == null || project.getName().isEmpty()) {
            return null;
        }
        String defaultLanguage = project.getDefaultLanguage();
        String byDefault = findNameByLanguage(project.getName(), defaultLanguage);
        if (byDefault != null) {
            return byDefault;
        }
        String byDecor = findNameByLanguage(project.getName(), decorLanguage);
        if (byDecor != null) {
            return byDecor;
        }
        return project.getName().getFirst().getValue();
    }

    private static String findNameByLanguage(List<BusinessNameWithLanguage> names, String language) {
        if (language == null || names == null) {
            return null;
        }
        for (BusinessNameWithLanguage name : names) {
            if (name == null || name.getValue() == null || name.getValue().isBlank()) {
                continue;
            }
            if (languageMatches(language, name.getLanguage())) {
                return name.getValue();
            }
        }
        return null;
    }

    private static boolean languageMatches(String expected, String actual) {
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

    private static CliOptions parseOptions(String[] args) {
        Path bbrPath = null;
        Path outputDir = null;
        Path cdaPackage = null;
        Path ansReference = null;
        Path configPath = null;
        String profilePrefix = null;
        String idPrefix = null;
        String titlePrefix = null;
        String resourcesDir = null;
        String invariantsDir = null;
        List<String> classificationTypes = null;
        List<String> templateIds = null;
        boolean allTemplates = false;
        boolean sushiRepo = false;
        String igId = null;
        String igName = null;
        String igTitle = null;
        String igCanonical = null;
        String igVersion = null;
        String igCopyrightYear = null;
        String igReleaseLabel = null;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if ("--bbr".equals(arg) && i + 1 < args.length) {
                bbrPath = Path.of(args[++i]);
            } else if ("--out".equals(arg) && i + 1 < args.length) {
                outputDir = Path.of(args[++i]);
            } else if ("--cda-package".equals(arg) && i + 1 < args.length) {
                cdaPackage = Path.of(args[++i]);
            } else if ("--ans-reference".equals(arg) && i + 1 < args.length) {
                ansReference = Path.of(args[++i]);
            } else if ("--config".equals(arg) && i + 1 < args.length) {
                configPath = Path.of(args[++i]);
            } else if ("--profile-prefix".equals(arg) && i + 1 < args.length) {
                profilePrefix = args[++i];
            } else if ("--id-prefix".equals(arg) && i + 1 < args.length) {
                idPrefix = args[++i];
            } else if ("--title-prefix".equals(arg) && i + 1 < args.length) {
                titlePrefix = args[++i];
            } else if ("--resources-dir".equals(arg) && i + 1 < args.length) {
                resourcesDir = args[++i];
            } else if ("--invariants-dir".equals(arg) && i + 1 < args.length) {
                invariantsDir = args[++i];
            } else if ("--classification-types".equals(arg) && i + 1 < args.length) {
                classificationTypes = parseCsv(args[++i]);
            } else if ("--template-ids".equals(arg) && i + 1 < args.length) {
                templateIds = parseCsv(args[++i]);
            } else if ("--all-templates".equals(arg)) {
                allTemplates = true;
            } else if ("--sushi-repo".equals(arg)) {
                sushiRepo = true;
            } else if ("--ig-id".equals(arg) && i + 1 < args.length) {
                igId = args[++i];
            } else if ("--ig-name".equals(arg) && i + 1 < args.length) {
                igName = args[++i];
            } else if ("--ig-title".equals(arg) && i + 1 < args.length) {
                igTitle = args[++i];
            } else if ("--ig-canonical".equals(arg) && i + 1 < args.length) {
                igCanonical = args[++i];
            } else if ("--ig-version".equals(arg) && i + 1 < args.length) {
                igVersion = args[++i];
            } else if ("--ig-copyright-year".equals(arg) && i + 1 < args.length) {
                igCopyrightYear = args[++i];
            } else if ("--ig-release-label".equals(arg) && i + 1 < args.length) {
                igReleaseLabel = args[++i];
            } else {
                System.err.println("Unknown or incomplete option: " + arg);
                printUsage();
                return null;
            }
        }

        if (bbrPath == null || outputDir == null) {
            System.err.println("Missing required options --bbr and --out");
            printUsage();
            return null;
        }
        if (ansReference != null && !Files.exists(ansReference)) {
            System.err.println("ANS reference path not found: " + ansReference);
        }
        return new CliOptions(bbrPath, outputDir, cdaPackage, ansReference, configPath, profilePrefix, idPrefix,
                titlePrefix, resourcesDir, invariantsDir, classificationTypes, templateIds, allTemplates,
                sushiRepo, igId, igName, igTitle, igCanonical, igVersion, igCopyrightYear, igReleaseLabel);
    }

    private static void printUsage() {
        System.out.println("Usage: axiom-cda generate --bbr <path> --out <dir> [options]");
        System.out.println("Options:");
        System.out.println("  --cda-package <dir>   Override CDA package directory");
        System.out.println("  --ans-reference <dir> Optional ANS FSH path (comparison only)");
        System.out.println("  --config <yaml>       Generation config override");
        System.out.println("  --profile-prefix <s>  Prefix profile names (default: none)");
        System.out.println("  --id-prefix <s>       Prefix profile ids (default: none)");
        System.out.println("  --title-prefix <s>    Prefix profile titles (default: none)");
        System.out.println("  --resources-dir <s>   Output folder for profiles (default: Resources<ProjectNameToken>)");
        System.out.println("  --invariants-dir <s>  Output folder for invariants (default: Invariants)");
        System.out.println("  --classification-types <csv>  Template classification filters");
        System.out.println("  --template-ids <csv>  Explicit template ids to generate");
        System.out.println("  --all-templates       Ignore classification filters and generate all");
        System.out.println("  --sushi-repo          Emit a SUSHI-ready repo (sushi-config.yaml + input/fsh)");
        System.out.println("  --ig-id <s>           IG id for sushi-config.yaml");
        System.out.println("  --ig-name <s>         IG name for sushi-config.yaml");
        System.out.println("  --ig-title <s>        IG title for sushi-config.yaml");
        System.out.println("  --ig-canonical <url>  IG canonical for sushi-config.yaml");
        System.out.println("  --ig-version <s>      IG version for sushi-config.yaml");
        System.out.println("  --ig-copyright-year <s>  IG copyrightYear for sushi-config.yaml");
        System.out.println("  --ig-release-label <s>   IG releaseLabel for sushi-config.yaml");
    }

    private static GenerationConfig applyNamingOverrides(GenerationConfig config, CliOptions options) {
        var existing = config.naming();
        String profilePrefix = options.profilePrefix != null ? options.profilePrefix : existing.profilePrefix();
        String idPrefix = options.idPrefix != null ? options.idPrefix : existing.idPrefix();
        String titlePrefix = options.titlePrefix != null ? options.titlePrefix : existing.titlePrefix();
        var naming = new net.ihe.gazelle.axiomcda.api.config.NamingConfig(
                profilePrefix,
                idPrefix,
                titlePrefix,
                existing.profileNameOverrides(),
                existing.idOverrides()
        );
        return new GenerationConfig(naming, config.nullFlavorPolicy(), config.valueSetPolicy(),
                config.templateSelection(), config.emitInvariants(), config.emitIrSnapshot());
    }

    private static GenerationConfig applySelectionOverrides(GenerationConfig config, CliOptions options) {
        TemplateSelection existing = config.templateSelection();
        List<String> templateIds = options.templateIds != null ? options.templateIds : existing.templateIds();
        List<String> classificationTypes = options.classificationTypes != null
                ? options.classificationTypes
                : existing.classificationTypes();
        if (options.allTemplates) {
            templateIds = List.of();
            classificationTypes = List.of();
        }
        TemplateSelection selection = new TemplateSelection(classificationTypes, templateIds);
        return new GenerationConfig(config.naming(), config.nullFlavorPolicy(), config.valueSetPolicy(),
                selection, config.emitInvariants(), config.emitIrSnapshot());
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

    private record CliOptions(Path bbrPath,
                              Path outputDir,
                              Path cdaPackage,
                              Path ansReference,
                              Path configPath,
                              String profilePrefix,
                              String idPrefix,
                              String titlePrefix,
                              String resourcesDir,
                              String invariantsDir,
                              List<String> classificationTypes,
                              List<String> templateIds,
                              boolean allTemplates,
                              boolean sushiRepo,
                              String igId,
                              String igName,
                              String igTitle,
                              String igCanonical,
                              String igVersion,
                              String igCopyrightYear,
                              String igReleaseLabel) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CdaPackageInfo(String name,
                                  String version,
                                  String canonical,
                                  List<String> fhirVersions,
                                  Map<String, String> dependencies) {
    }
}
