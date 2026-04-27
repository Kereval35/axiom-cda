package net.ihe.gazelle.axiomcda.engine.business;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureMapSemanticAnalyzerTest {

    @Test
    void analyzesObservationBranchesAndDependentGroups() throws Exception {
        String structureMap = readFixture("observation/clean-observation-structuremap.json");

        StructureMapSemanticAnalyzer analyzer = new StructureMapSemanticAnalyzer();
        StructureMapSemanticAnalyzer.StructureMapSemanticModel model = analyzer.analyze(structureMap);

        BranchInferenceEngine engine = new BranchInferenceEngine();
        List<BranchInferenceEngine.BranchInference> inferences = engine.infer(model);

        assertTrue(inferences.stream().anyMatch(inference ->
                "participant".equals(inference.sourceBranch())
                        && inference.targetsObservationRoot("performer")));
        assertTrue(inferences.stream().anyMatch(inference ->
                "entryRelationship".equals(inference.sourceBranch())
                        && inference.targetsObservationRoot("note")));
        assertTrue(inferences.stream().anyMatch(inference ->
                "referenceRange".equals(inference.sourceBranch())
                        && inference.targetsObservationRoot("referenceRange")));
        assertTrue(inferences.stream().anyMatch(inference ->
                "value".equals(inference.sourceBranch())
                        && inference.alternatives().stream().anyMatch(alternative ->
                        "PQ".equals(alternative.sourceType()) && "Quantity".equals(alternative.createdType()))));
    }

    private String readFixture(String path) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing test fixture: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
