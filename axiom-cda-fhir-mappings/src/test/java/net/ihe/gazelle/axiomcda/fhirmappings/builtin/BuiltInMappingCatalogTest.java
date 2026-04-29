package net.ihe.gazelle.axiomcda.fhirmappings.builtin;

import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;
import net.ihe.gazelle.axiomcda.fhirmappings.structuremap.StructureMapSemanticAnalyzer;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BuiltInMappingCatalogTest {

    @Test
    void ehdsiObservationPackKeepsCurrentStructureMapSemanticsAndAddsPriorResultSupport() throws Exception {
        SemanticMappingModel builtIn = new BuiltInMappingModelProvider().resolve("Observation", "observation-ehdsi-lab-v1");
        String structureMap = readFixture("observation/clean-observation-structuremap.json");
        SemanticMappingModel uploaded = new StructureMapSemanticAnalyzer().analyze(structureMap);

        assertTrue(builtIn.allRules().stream()
                .flatMap(rule -> rule.targets().stream())
                .anyMatch(target -> "meta.profile".equals(target.path())));
        assertTrue(uploaded.allRules().stream()
                .flatMap(rule -> rule.targets().stream())
                .anyMatch(target -> "note".equals(target.path())));
        assertTrue(builtIn.allRules().stream()
                .flatMap(rule -> rule.targets().stream())
                .anyMatch(target -> "note".equals(target.path())));
        assertTrue(builtIn.allRules().stream()
                .anyMatch(rule -> "entryRelationship.observation".equals(rule.primarySourcePath())
                        && rule.targets().stream().anyMatch(target -> "hasMember".equals(target.path()))));
    }

    @Test
    void genericObservationPackDoesNotForceEhdsiParentProfile() {
        SemanticMappingModel builtIn = new BuiltInMappingModelProvider().resolve("Observation", "observation-hl7-core-v1");

        assertFalse(builtIn.allRules().stream()
                .flatMap(rule -> rule.targets().stream())
                .anyMatch(target -> "meta.profile".equals(target.path())));
        assertTrue(builtIn.allRules().stream().allMatch(rule -> rule.mappingKind() != null));
        assertTrue(builtIn.allRules().stream()
                .filter(rule -> "FhirObservationCategoryCoding".equals(rule.name()))
                .allMatch(rule -> "GLOBAL".equals(rule.displaySourceLabel())));
        assertTrue(builtIn.allRules().stream()
                .anyMatch(rule -> "entryRelationship.observation".equals(rule.primarySourcePath())
                        && rule.targets().stream().anyMatch(target -> "hasMember".equals(target.path()))));
    }

    private String readFixture(String path) throws Exception {
        Path fixture = Path.of("..", "axiom-cda-ws", "src", "test", "resources", path).normalize();
        return Files.readString(fixture, StandardCharsets.UTF_8);
    }
}
