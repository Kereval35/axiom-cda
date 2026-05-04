package net.ihe.gazelle.axiomcda.fhirmappings.compact;

import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticRule;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationMappingCompilerTest {

    @Test
    void compilesObservationMappingIntoSemanticRules() throws Exception {
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("fhir-mappings/observation/observation-core-v2.yaml")) {
            ObservationMapping mapping = new ObservationMappingLoader().load(stream);
            SemanticMappingModel model = new ObservationMappingCompiler().compile(mapping);

            List<SemanticRule> rules = model.allRules();
            assertFalse(rules.isEmpty());

            assertTrue(rules.stream().anyMatch(rule ->
                    "id".equals(rule.primarySourcePath())
                            && rule.targets().stream().anyMatch(target -> "identifier".equals(target.path()))));

            assertTrue(rules.stream().anyMatch(rule ->
                    rule.primarySourcePath().isBlank()
                            && rule.targets().stream().anyMatch(target ->
                            "meta.profile".equals(target.path())
                                    && "http://hl7.org/fhir/StructureDefinition/Observation".equals(target.constantValue()))));

            assertTrue(rules.stream().anyMatch(rule ->
                    "code.codeSystem".equals(rule.primarySourcePath())
                            && rule.targets().stream().anyMatch(target -> "coding.system".equals(target.path())
                            || "code.coding.system".equals(target.path()))));

            assertTrue(rules.stream().anyMatch(rule ->
                    "statusCode.code".equals(rule.primarySourcePath())
                            && rule.targets().stream().anyMatch(target ->
                            "status".equals(target.path())
                                    && "translate".equals(target.transform()))));

            assertEquals(0, rules.stream().filter(rule ->
                    "statusCode".equals(rule.primarySourcePath())
                            && rule.targets().stream().anyMatch(target -> "status".equals(target.path()))).count());

            assertTrue(rules.stream().anyMatch(rule ->
                    "effectiveTime".equals(rule.primarySourcePath())
                            && rule.targets().stream().anyMatch(target ->
                            "effective".equals(target.path())
                                    && "dateTime".equals(target.createdType())
                                    && "dateTime".equals(target.constantValue())
                                    && "create".equals(target.transform()))));

            assertTrue(rules.stream().anyMatch(rule ->
                    "value".equals(rule.primarySourcePath())
                            && rule.sources().stream().anyMatch(source -> "PQ".equals(source.type()))
                            && rule.targets().stream().anyMatch(target ->
                            "value".equals(target.path())
                                    && "Quantity".equals(target.createdType())
                                    && "Quantity".equals(target.constantValue())
                                    && "create".equals(target.transform()))));

            assertTrue(rules.stream().anyMatch(rule ->
                    "entryRelationship.observation".equals(rule.primarySourcePath())
                            && rule.targets().stream().anyMatch(target ->
                            "hasMember".equals(target.path())
                                    && "Reference".equals(target.createdType())
                                    && "Reference".equals(target.constantValue())
                                    && "create".equals(target.transform()))));

            assertNotNull(model.groups());
            assertFalse(model.groups().isEmpty());
        }
    }
}
