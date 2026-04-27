package net.ihe.gazelle.axiomcda.engine.business;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationStructureMapFlattenerTest {

    @Test
    void flattensNestedObservationTargets() throws Exception {
        String structureMap = readFixture("observation/clean-observation-structuremap.json");

        ObservationStructureMapFlattener flattener = new ObservationStructureMapFlattener();
        List<ObservationStructureMapFlattener.FlattenedStructureMapOperation> operations = flattener.flatten(structureMap);

        assertTrue(operations.stream().anyMatch(operation ->
                "meta.profile".equals(operation.targetPath())
                        && "http://fhir.ehdsi.eu/laboratory/StructureDefinition/Observation-resultslab-lab-myhealtheu".equals(operation.constantValue())));
        assertTrue(operations.stream().anyMatch(operation ->
                "interpretationCode".equals(operation.sourcePath())
                        && "interpretation".equals(operation.targetPath())));
        assertTrue(operations.stream().anyMatch(operation ->
                "value".equals(operation.sourcePath())
                        && "value".equals(operation.targetPath())
                        && "PQ".equals(operation.sourceType())
                        && "Quantity".equals(operation.createdType())));
    }

    private String readFixture(String path) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing test fixture: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
