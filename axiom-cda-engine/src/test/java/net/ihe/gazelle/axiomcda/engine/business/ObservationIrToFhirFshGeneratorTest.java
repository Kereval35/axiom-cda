package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.ir.*;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;
import net.ihe.gazelle.axiomcda.fhirmappings.builtin.BuiltInMappingModelProvider;
import net.ihe.gazelle.axiomcda.fhirmappings.structuremap.StructureMapSemanticAnalyzer;
import net.ihe.gazelle.axiomcda.fhirmappings.trace.SemanticMappingFshTraceRenderer;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationIrToFhirFshGeneratorTest {

    @Test
    void generatesObservationFhirProfileFromIrAndStructureMap() throws Exception {
        IRTemplate template = new IRTemplate(
                "obs-template",
                "Observation Result",
                "Observation Result",
                "Observation Result",
                "Observation",
                List.of(
                        new IRElementConstraint("id", new IRCardinality(1, "*"), null, null, null, List.of(), null),
                        new IRElementConstraint("code", new IRCardinality(1, "1"), null, null, null,
                                List.of(new IRBinding(IRBindingStrength.REQUIRED, "http://example.org/ValueSet/obs-code", null)), "Observation code"),
                        new IRElementConstraint("code.code", null, null, "12345-6", IRFixedValueType.CODE, List.of(), null),
                        new IRElementConstraint("code.codeSystem", null, null, "http://loinc.org", IRFixedValueType.STRING, List.of(), null),
                        new IRElementConstraint("effectiveTime", new IRCardinality(0, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("statusCode", new IRCardinality(1, "1"), null, null, null, List.of(), "Observation status"),
                        new IRElementConstraint("statusCode.code", null, null, "final", IRFixedValueType.CODE, List.of(), null),
                        new IRElementConstraint("text", new IRCardinality(0, "1"), "ED", null, null, List.of(), "Narrative comment"),
                        new IRElementConstraint("interpretationCode", new IRCardinality(0, "*"), null, null, null,
                                List.of(new IRBinding(IRBindingStrength.EXTENSIBLE, "http://example.org/ValueSet/interp", null)), "Interpretation"),
                        new IRElementConstraint("interpretationCode.code", null, null, "N", IRFixedValueType.CODE, List.of(), null),
                        new IRElementConstraint("code.originalText", new IRCardinality(0, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("code.translation", new IRCardinality(0, "*"), null, null, null, List.of(), null),
                        new IRElementConstraint("methodCode", new IRCardinality(0, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("performer", new IRCardinality(0, "*"), null, null, null, List.of(), "Performer"),
                        new IRElementConstraint("participant", new IRCardinality(0, "*"), null, null, null, List.of(), "Participant"),
                        new IRElementConstraint("author", new IRCardinality(0, "*"), null, null, null, List.of(), null),
                        new IRElementConstraint("entryRelationship", new IRCardinality(0, "*"), null, null, null, List.of(), "Comment entries"),
                        new IRElementConstraint("entryRelationship.typeCode", new IRCardinality(0, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("entryRelationship.inversionInd", new IRCardinality(0, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("reference", new IRCardinality(0, "*"), null, null, null, List.of(), null),
                        new IRElementConstraint("referenceRange", new IRCardinality(0, "*"), null, null, null, List.of(), null),
                        new IRElementConstraint("referenceRange.observationRange.value.low", new IRCardinality(0, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("value", new IRCardinality(0, "1"), "PQ", null, null, List.of(), null),
                        new IRElementConstraint("value", new IRCardinality(0, "1"), "CD", null, null,
                                List.of(new IRBinding(IRBindingStrength.PREFERRED, "http://example.org/ValueSet/value-code", null)), "Coded value"),
                        new IRElementConstraint("value", new IRCardinality(0, "1"), "ANY", null, null, List.of(), null)
                ),
                List.of(),
                List.of()
        );

        String structureMap = readFixture("observation/clean-observation-structuremap.json");

        SemanticMappingModel semanticModel = new StructureMapSemanticAnalyzer().analyze(structureMap);

        ObservationIrToFhirFshGenerator generator = new ObservationIrToFhirFshGenerator();
        ObservationFhirConversionResult result = generator.generate(template, "ObservationResult", semanticModel);

        assertTrue(result.fsh().contains("Parent: http://fhir.ehdsi.eu/laboratory/StructureDefinition/Observation-resultslab-lab-myhealtheu"));
        assertTrue(result.fsh().contains("* category.coding.code = #laboratory"));
        assertTrue(result.fsh().contains("* identifier 1..*"));
        assertTrue(result.fsh().contains("* code 1..1"));
        assertTrue(result.fsh().contains("* code from http://example.org/ValueSet/obs-code (required)"));
        assertTrue(result.fsh().contains("* code.coding.code = #12345-6"));
        assertTrue(result.fsh().contains("* code.coding.system = \"http://loinc.org\""));
        assertTrue(result.fsh().contains("* code.text 0..1"));
        assertTrue(result.fsh().contains("* code.coding 0..*"));
        assertTrue(result.fsh().contains("* effective[x] only dateTime"));
        assertTrue(result.fsh().contains("* status 1..1"));
        assertTrue(result.fsh().contains("* status = #final"));
        assertTrue(result.fsh().contains("* note 0..1"));
        assertTrue(result.fsh().contains("* note.text ^short = \"Narrative comment\""));
        assertTrue(result.fsh().contains("* interpretation from http://example.org/ValueSet/interp (extensible)"));
        assertTrue(result.fsh().contains("* interpretation.coding.code = #N"));
        assertTrue(result.fsh().contains("* performer 0..*"));
        assertFalse(result.fsh().contains("* note 0..*"));
        assertTrue(result.fsh().contains("* referenceRange 0..*"));
        assertTrue(result.fsh().contains("* referenceRange.low 0..1"));
        assertTrue(result.fsh().contains("* value[x] only Quantity"));
        assertTrue(result.fsh().contains("* value[x] only CodeableConcept"));
        assertTrue(result.fsh().contains("* valueCodeableConcept from http://example.org/ValueSet/value-code (preferred)"));
        assertTrue(result.fsh().contains("* value[x] ^short = \"Coded value\""));
        assertTrue(result.diagnostics().stream().anyMatch(message -> message.contains("does not expose a safe Observation.method branch")));
        assertTrue(result.diagnostics().stream().anyMatch(message -> message.contains("datatype 'ANY'")));
        assertTrue(result.diagnostics().stream().anyMatch(message -> message.contains("reference") && message.contains("explicit relationship policy")));
        assertFalse(result.diagnostics().stream().anyMatch(message -> message.contains("code.originalText")));
        assertFalse(result.diagnostics().stream().anyMatch(message -> message.contains("performer")));
        assertFalse(result.diagnostics().stream().anyMatch(message -> message.contains("author")));
        assertFalse(result.diagnostics().stream().anyMatch(message -> message.contains("entryRelationship.typeCode")));
        assertFalse(result.diagnostics().stream().anyMatch(message -> message.contains("entryRelationship.inversionInd")));

        String fullTrace = new SemanticMappingFshTraceRenderer().render("FullRules", "Observation", "test", semanticModel);
        String usedTrace = new SemanticMappingFshTraceRenderer().render("UsedRules", "Observation", "test", result.usedMappingModel());
        assertTrue(usedTrace.contains("// id -> identifier"));
        assertTrue(usedTrace.contains("// effectiveTime -> effective"));
        assertFalse(usedTrace.contains("GLOBAL -> subject"));
        assertTrue(fullTrace.contains("GLOBAL -> subject"));
    }

    @Test
    void builtInCoreMappingProjectsRelatedObservationEntryRelationshipToHasMember() throws Exception {
        IRTemplate template = new IRTemplate(
                "obs-template",
                "Observation Result",
                "Observation Result",
                "Observation Result",
                "Observation",
                List.of(
                        new IRElementConstraint("entryRelationship.observation", new IRCardinality(0, "*"), null, null, null, List.of(), "Prior results"),
                        new IRElementConstraint("entryRelationship.observation.statusCode", new IRCardinality(1, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("entryRelationship.observation.effectiveTime", new IRCardinality(1, "1"), null, null, null, List.of(), null)
                ),
                List.of(),
                List.of()
        );

        SemanticMappingModel semanticModel = new BuiltInMappingModelProvider().resolve("Observation", "observation-hl7-core-v1");

        ObservationFhirConversionResult result = new ObservationIrToFhirFshGenerator()
                .generate(template, "ObservationResult", semanticModel);

        assertTrue(result.fsh().contains("* hasMember 0..*"));
        assertFalse(result.diagnostics().stream().anyMatch(message -> message.contains("path 'entryRelationship.observation' is not emitted as a standalone")));
        assertFalse(result.diagnostics().stream().anyMatch(message -> message.contains("entryRelationship.observation.statusCode") && message.contains("runtime relationship construction")));

        String usedTrace = new SemanticMappingFshTraceRenderer().render("UsedRules", "Observation", "test", result.usedMappingModel());
        assertTrue(usedTrace.contains("entryRelationship.observation -> hasMember"));
    }

    private String readFixture(String path) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing test fixture: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
