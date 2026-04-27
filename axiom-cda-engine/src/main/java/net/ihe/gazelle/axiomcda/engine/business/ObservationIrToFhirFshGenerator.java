package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.api.ir.IRCardinality;
import net.ihe.gazelle.axiomcda.engine.util.FshUtil;
import net.ihe.gazelle.axiomcda.engine.util.NameUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class ObservationIrToFhirFshGenerator {

    public ObservationFhirConversionResult generate(IRTemplate template,
                                                    String sourceProfileName,
                                                    String structureMapJson) throws IOException {
        if (template == null) {
            throw new IllegalArgumentException("template must be set");
        }
        if (!"Observation".equals(template.rootCdaType())) {
            throw new IllegalArgumentException("Only Observation templates are supported in this PoC");
        }

        StructureMapSemanticAnalyzer analyzer = new StructureMapSemanticAnalyzer();
        StructureMapSemanticAnalyzer.StructureMapSemanticModel semanticModel = analyzer.analyze(structureMapJson);

        BranchInferenceEngine inferenceEngine = new BranchInferenceEngine();
        List<BranchInferenceEngine.BranchInference> inferences = inferenceEngine.infer(semanticModel);

        ObservationSemanticInterpreter interpreter = new ObservationSemanticInterpreter();
        ObservationSemanticInterpreter.ObservationProjectionResult projection = interpreter.interpret(template, semanticModel, inferences);

        String profileName = buildProfileName(sourceProfileName, template);
        String profileId = NameUtil.toKebabCase(profileName);
        String description = template.description() == null || template.description().isBlank()
                ? "FHIR Observation profile generated from CDA IR and StructureMap PoC."
                : template.description();

        LinkedHashSet<String> lines = new LinkedHashSet<>();
        lines.add("Profile: " + profileName);
        lines.add("Parent: " + projection.parent());
        lines.add("Id: " + profileId);
        lines.add("Title: \"" + FshUtil.escape(profileName) + "\"");
        lines.add("Description: \"" + FshUtil.escape(description) + "\"");
        lines.add("* ^status = #draft");
        normalizeCandidates(projection.candidates()).stream()
                .map(ObservationSemanticInterpreter.ProjectionCandidate::toFshLine)
                .forEach(lines::add);

        List<String> diagnostics = projection.diagnostics().stream()
                .map(ObservationSemanticInterpreter.ProjectionDiagnostic::message)
                .toList();

        return new ObservationFhirConversionResult(profileName, String.join("\n", lines) + "\n", diagnostics);
    }

    private List<ObservationSemanticInterpreter.ProjectionCandidate> normalizeCandidates(
            List<ObservationSemanticInterpreter.ProjectionCandidate> candidates) {
        Map<String, IRCardinality> cardinalitiesByPath = new LinkedHashMap<>();
        for (ObservationSemanticInterpreter.ProjectionCandidate candidate : candidates) {
            if (candidate.kind() == ObservationSemanticInterpreter.ProjectionKind.CARDINALITY) {
                cardinalitiesByPath.merge(candidate.targetPath(), parseCardinality(candidate.value()), this::mergeCardinality);
            }
        }

        List<ObservationSemanticInterpreter.ProjectionCandidate> normalized = new ArrayList<>();
        LinkedHashSet<String> emittedCardinalityPaths = new LinkedHashSet<>();
        for (ObservationSemanticInterpreter.ProjectionCandidate candidate : candidates) {
            if (candidate.kind() != ObservationSemanticInterpreter.ProjectionKind.CARDINALITY) {
                normalized.add(candidate);
                continue;
            }
            if (emittedCardinalityPaths.add(candidate.targetPath())) {
                normalized.add(ObservationSemanticInterpreter.ProjectionCandidate.cardinality(
                        candidate.targetPath(),
                        cardinalitiesByPath.get(candidate.targetPath())
                ));
            }
        }
        return normalized;
    }

    private IRCardinality parseCardinality(String value) {
        String[] parts = value.split("\\.\\.", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid cardinality: " + value);
        }
        return new IRCardinality(Integer.parseInt(parts[0]), parts[1]);
    }

    private IRCardinality mergeCardinality(IRCardinality first, IRCardinality second) {
        int min = Math.max(first.min(), second.min());
        String max = minMax(first.max(), second.max());
        if (!"*".equals(max) && Integer.parseInt(max) < min) {
            max = String.valueOf(min);
        }
        return new IRCardinality(min, max);
    }

    private String minMax(String first, String second) {
        if ("*".equals(first)) {
            return second;
        }
        if ("*".equals(second)) {
            return first;
        }
        return String.valueOf(Math.min(Integer.parseInt(first), Integer.parseInt(second)));
    }

    private String buildProfileName(String sourceProfileName, IRTemplate template) {
        String base = (sourceProfileName == null || sourceProfileName.isBlank())
                ? template.rootCdaType()
                : sourceProfileName;
        String sanitized = base.replaceAll("[^A-Za-z0-9]", "");
        return sanitized + "FhirObservation";
    }
}
