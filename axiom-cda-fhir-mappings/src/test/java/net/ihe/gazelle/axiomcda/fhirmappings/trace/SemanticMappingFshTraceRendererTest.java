package net.ihe.gazelle.axiomcda.fhirmappings.trace;

import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;
import net.ihe.gazelle.axiomcda.fhirmappings.builtin.BuiltInMappingModelProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticMappingFshTraceRendererTest {

    @Test
    void rendersEffectiveObservationRulesAsHumanReadableMappingTrace() {
        SemanticMappingModel model = new BuiltInMappingModelProvider()
                .resolve("Observation", "observation-hl7-core-v1");

        String rendered = new SemanticMappingFshTraceRenderer().render(
                "ObservationMappingRules",
                "Observation",
                "built-in:observation-hl7-core-v1",
                model
        );

        assertTrue(rendered.contains("RuleSet: ObservationMappingRules"));
        assertTrue(rendered.contains("* metadata.rootCdaType = \"Observation\""));
        assertTrue(rendered.contains("// id -> identifier"));
        assertTrue(rendered.contains("* groupName = \"CdaLaboratoryObservationToFhirObservation\""));
        assertTrue(rendered.contains("* ruleName = \"id\""));
        assertTrue(rendered.contains("* from = \"id\""));
        assertTrue(rendered.contains("* to = \"identifier\""));
        assertTrue(rendered.contains("* mappingKind = #DIRECT_PATH"));
    }
}
