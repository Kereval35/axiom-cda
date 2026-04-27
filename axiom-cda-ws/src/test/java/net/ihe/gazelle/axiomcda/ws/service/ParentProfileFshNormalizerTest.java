package net.ihe.gazelle.axiomcda.ws.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParentProfileFshNormalizerTest {

    @Test
    void adjustsCardinalitiesToFitCachedParentProfile() throws Exception {
        String packageId = "axiom.test.parent." + System.nanoTime();
        String packageVersion = "1.0.0";
        String parentUrl = "http://example.org/fhir/StructureDefinition/ParentObservation";
        Path packageDir = Path.of(System.getProperty("java.io.tmpdir"), ".fhir", "packages", packageId + "#" + packageVersion, "package");
        Files.createDirectories(packageDir);
        Files.writeString(packageDir.resolve("StructureDefinition-ParentObservation.json"), """
                {
                  "resourceType": "StructureDefinition",
                  "id": "ParentObservation",
                  "url": "http://example.org/fhir/StructureDefinition/ParentObservation",
                  "snapshot": {
                    "element": [
                      { "id": "Observation", "path": "Observation", "min": 0, "max": "*" },
                      { "id": "Observation.effective[x]", "path": "Observation.effective[x]", "min": 1, "max": "1" },
                      { "id": "Observation.note", "path": "Observation.note", "min": 0, "max": "1" }
                    ]
                  }
                }
                """, StandardCharsets.UTF_8);

        ParentProfileFshNormalizer.NormalizationResult result = new ParentProfileFshNormalizer().normalize("""
                Profile: ChildObservation
                Parent: http://example.org/fhir/StructureDefinition/ParentObservation
                * effective[x] 0..1
                * note 0..*
                * status 1..1
                """, parentUrl, false, packageId, packageVersion);

        assertTrue(result.fshContent().contains("* effective[x] 1..1"));
        assertTrue(result.fshContent().contains("* note 0..1"));
        assertTrue(result.fshContent().contains("* status 1..1"));
        assertFalse(result.fshContent().contains("* effective[x] 0..1"));
        assertFalse(result.fshContent().contains("* note 0..*"));
        assertTrue(result.diagnostics().stream().anyMatch(message -> message.contains("Adjusted effective[x] cardinality")));
        assertTrue(result.diagnostics().stream().anyMatch(message -> message.contains("Adjusted note cardinality")));
    }
}
