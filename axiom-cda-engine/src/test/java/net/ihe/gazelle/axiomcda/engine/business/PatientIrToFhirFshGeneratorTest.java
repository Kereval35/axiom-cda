package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.ir.IRCardinality;
import net.ihe.gazelle.axiomcda.api.ir.IRElementConstraint;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;
import net.ihe.gazelle.axiomcda.fhirmappings.structuremap.StructureMapSemanticAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientIrToFhirFshGeneratorTest {

    @Test
    void infersPatientParentAndIgnoresMappedContainerBranches() throws Exception {
        IRTemplate template = new IRTemplate(
                "record-target-template",
                "CI-SIS RecordTarget",
                "CI-SIS RecordTarget",
                "CI-SIS RecordTarget",
                "RecordTarget",
                List.of(
                        new IRElementConstraint("patientRole", new IRCardinality(1, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("patientRole.id", new IRCardinality(1, "*"), null, null, null, List.of(), null),
                        new IRElementConstraint("patientRole.patient", new IRCardinality(1, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("patientRole.patient.administrativeGenderCode", new IRCardinality(1, "1"), null, null, null, List.of(), null)
                ),
                List.of(),
                List.of()
        );
        SemanticMappingModel model = new StructureMapSemanticAnalyzer().analyze(patientStructureMap());

        ObservationFhirConversionResult result = new PatientIrToFhirFshGenerator()
                .generate(template, "RecordTargetCISISRecordTarget", model);

        assertTrue(result.fsh().contains("Parent: http://hl7.org/fhir/StructureDefinition/Patient"));
        assertTrue(result.fsh().contains("Profile: RecordTargetCISISRecordTargetFhirPatient"));
        assertTrue(result.fsh().contains("* identifier 1..*"));
        assertTrue(result.fsh().contains("* gender 1..1"));
        assertFalse(result.diagnostics().stream().anyMatch(message -> message.contains("patientRole")));
        assertFalse(result.usedMappingModel().allRules().isEmpty());
    }

    @Test
    void acceptsLiteralBranchContextForSiblingPatientRules() throws Exception {
        IRTemplate template = new IRTemplate(
                "record-target-template",
                "CI-SIS RecordTarget",
                "CI-SIS RecordTarget",
                "CI-SIS RecordTarget",
                "RecordTarget",
                List.of(
                        new IRElementConstraint("patientRole", new IRCardinality(1, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("patientRole.id", new IRCardinality(1, "*"), null, null, null, List.of(), null),
                        new IRElementConstraint("patientRole.patient.administrativeGenderCode", new IRCardinality(1, "1"), null, null, null, List.of(), null)
                ),
                List.of(),
                List.of()
        );
        SemanticMappingModel model = new StructureMapSemanticAnalyzer().analyze(patientStructureMapUsingLiteralContext());

        ObservationFhirConversionResult result = new PatientIrToFhirFshGenerator()
                .generate(template, "RecordTargetCISISRecordTarget", model);

        assertTrue(result.fsh().contains("Parent: http://hl7.org/fhir/StructureDefinition/Patient"));
        assertTrue(result.fsh().contains("* identifier 1..*"));
        assertTrue(result.fsh().contains("* gender 1..1"));
        assertFalse(result.diagnostics().stream().anyMatch(message -> message.contains("patientRole")));
    }

    @Test
    void resolvesAliasedTargetTypeFromStructureDefinitions() throws Exception {
        IRTemplate template = new IRTemplate(
                "record-target-template",
                "CI-SIS RecordTarget",
                "CI-SIS RecordTarget",
                "CI-SIS RecordTarget",
                "RecordTarget",
                List.of(
                        new IRElementConstraint("patientRole", new IRCardinality(1, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("patientRole.id", new IRCardinality(1, "*"), null, null, null, List.of(), null),
                        new IRElementConstraint("patientRole.patient.administrativeGenderCode", new IRCardinality(1, "1"), null, null, null, List.of(), null)
                ),
                List.of(),
                List.of()
        );
        SemanticMappingModel model = new StructureMapSemanticAnalyzer().analyze(patientStructureMapUsingAliasedTargetType());

        ObservationFhirConversionResult result = new PatientIrToFhirFshGenerator()
                .generate(template, "RecordTargetCISISRecordTarget", model);

        assertTrue(result.fsh().contains("Parent: http://hl7.org/fhir/StructureDefinition/Patient"));
        assertTrue(result.fsh().contains("Profile: RecordTargetCISISRecordTargetFhirPatient"));
        assertTrue(result.fsh().contains("* identifier 1..*"));
        assertTrue(result.fsh().contains("* gender 1..1"));
        assertFalse(result.diagnostics().stream().anyMatch(message -> message.contains("patientRole")));
    }

    @Test
    void infersCommonPatientAddressPathFromMappedPatientRoleBranch() throws Exception {
        IRTemplate template = new IRTemplate(
                "record-target-template",
                "CI-SIS RecordTarget",
                "CI-SIS RecordTarget",
                "CI-SIS RecordTarget",
                "RecordTarget",
                List.of(
                        new IRElementConstraint("patientRole", new IRCardinality(1, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("patientRole.id", new IRCardinality(1, "*"), null, null, null, List.of(), null),
                        new IRElementConstraint("patientRole.addr", new IRCardinality(0, "*"), null, null, null, List.of(), null)
                ),
                List.of(),
                List.of()
        );
        SemanticMappingModel model = new StructureMapSemanticAnalyzer().analyze(patientStructureMapUsingAliasedTargetType());

        ObservationFhirConversionResult result = new PatientIrToFhirFshGenerator()
                .generate(template, "RecordTargetCISISRecordTarget", model);

        assertTrue(result.fsh().contains("* address 0..*"));
        assertFalse(result.diagnostics().stream().anyMatch(message -> message.contains("CDA path 'patientRole.addr'")));
    }

    @Test
    void infersNestedPatientNameAndAddressChildrenFromCommonPathSuffixes() throws Exception {
        IRTemplate template = new IRTemplate(
                "record-target-template",
                "CI-SIS RecordTarget",
                "CI-SIS RecordTarget",
                "CI-SIS RecordTarget",
                "RecordTarget",
                List.of(
                        new IRElementConstraint("patientRole", new IRCardinality(1, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("patientRole.patient.name", new IRCardinality(1, "*"), null, null, null, List.of(), null),
                        new IRElementConstraint("patientRole.patient.name.item.given", new IRCardinality(1, "*"), null, null, null, List.of(), null),
                        new IRElementConstraint("patientRole.patient.name.family", new IRCardinality(1, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("patientRole.addr.city", new IRCardinality(0, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("patientRole.addr.postalCode", new IRCardinality(0, "1"), null, null, null, List.of(), null)
                ),
                List.of(),
                List.of()
        );
        SemanticMappingModel model = new StructureMapSemanticAnalyzer().analyze(patientStructureMapUsingAliasedTargetType());

        ObservationFhirConversionResult result = new PatientIrToFhirFshGenerator()
                .generate(template, "RecordTargetCISISRecordTarget", model);

        assertTrue(result.fsh().contains("* name 1..*"));
        assertTrue(result.fsh().contains("* name.given 1..*"));
        assertTrue(result.fsh().contains("* name.family 1..1"));
        assertTrue(result.fsh().contains("* address.city 0..1"));
        assertTrue(result.fsh().contains("* address.postalCode 0..1"));
        assertFalse(result.fsh().contains("name.item.given"));
        assertFalse(result.diagnostics().stream().anyMatch(message -> message.contains("patientRole")));
    }

    @Test
    void infersIdentifierRootAndExtensionAsSystemAndValue() throws Exception {
        IRTemplate template = new IRTemplate(
                "record-target-template",
                "CI-SIS RecordTarget",
                "CI-SIS RecordTarget",
                "CI-SIS RecordTarget",
                "RecordTarget",
                List.of(
                        new IRElementConstraint("patientRole.id", new IRCardinality(1, "*"), null, null, null, List.of(), null),
                        new IRElementConstraint("patientRole.id.root", new IRCardinality(0, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("patientRole.id.extension", new IRCardinality(0, "1"), null, null, null, List.of(), null)
                ),
                List.of(),
                List.of()
        );
        SemanticMappingModel model = new StructureMapSemanticAnalyzer().analyze(patientStructureMapUsingAliasedTargetType());

        ObservationFhirConversionResult result = new PatientIrToFhirFshGenerator()
                .generate(template, "RecordTargetCISISRecordTarget", model);

        assertTrue(result.fsh().contains("* identifier 1..*"));
        assertTrue(result.fsh().contains("* identifier.system 0..1"));
        assertTrue(result.fsh().contains("* identifier.value 0..1"));
        assertFalse(result.fsh().contains("identifier.root"));
        assertFalse(result.fsh().contains("identifier.extension"));
    }

    @Test
    void doesNotInferInvalidQualifierChildUnderHumanName() throws Exception {
        IRTemplate template = new IRTemplate(
                "record-target-template",
                "CI-SIS RecordTarget",
                "CI-SIS RecordTarget",
                "CI-SIS RecordTarget",
                "RecordTarget",
                List.of(
                        new IRElementConstraint("patientRole.patient.name.item.given.qualifier", new IRCardinality(0, "1"), null, null, null, List.of(), null)
                ),
                List.of(),
                List.of()
        );
        SemanticMappingModel model = new StructureMapSemanticAnalyzer().analyze(patientStructureMapUsingAliasedTargetType());

        ObservationFhirConversionResult result = new PatientIrToFhirFshGenerator()
                .generate(template, "RecordTargetCISISRecordTarget", model);

        assertFalse(result.fsh().contains("qualifier"));
        assertTrue(result.diagnostics().isEmpty());
    }

    private String patientStructureMap() {
        return """
                {
                  "resourceType": "StructureMap",
                  "group": [
                    {
                      "name": "RecordTargetToPatient",
                      "input": [
                        { "name": "src", "mode": "source", "type": "RecordTarget" },
                        { "name": "tgt", "mode": "target", "type": "Patient" }
                      ],
                      "rule": [
                        {
                          "name": "patientRole",
                          "source": [
                            { "context": "src", "element": "patientRole", "variable": "pr" }
                          ],
                          "rule": [
                            {
                              "name": "id",
                              "source": [
                                { "context": "pr", "element": "id", "variable": "id" }
                              ],
                              "target": [
                                { "context": "tgt", "element": "identifier", "variable": "ident" }
                              ]
                            },
                            {
                              "name": "patient",
                              "source": [
                                { "context": "pr", "element": "patient", "variable": "pt" }
                              ],
                              "rule": [
                                {
                                  "name": "administrativeGenderCode",
                                  "source": [
                                    { "context": "pt", "element": "administrativeGenderCode", "variable": "admg" }
                                  ],
                                  "target": [
                                    { "context": "tgt", "element": "gender", "transform": "copy", "parameter": [ { "valueId": "admg" } ] }
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private String patientStructureMapUsingLiteralContext() {
        return """
                {
                  "resourceType": "StructureMap",
                  "group": [
                    {
                      "name": "RecordTargetToPatient",
                      "input": [
                        { "name": "src", "mode": "source", "type": "RecordTarget" },
                        { "name": "tgt", "mode": "target", "type": "Patient" }
                      ],
                      "rule": [
                        {
                          "name": "patientRole",
                          "source": [
                            { "context": "src", "element": "patientRole", "variable": "pr" }
                          ],
                          "rule": [
                            {
                              "name": "id",
                              "source": [
                                { "context": "pr", "element": "id", "variable": "id" }
                              ],
                              "target": [
                                { "context": "tgt", "element": "identifier", "variable": "ident" }
                              ]
                            }
                          ]
                        },
                        {
                          "name": "patient",
                          "source": [
                            { "context": "patientRole", "element": "patient", "variable": "pt" }
                          ],
                          "rule": [
                            {
                              "name": "administrativeGenderCode",
                              "source": [
                                { "context": "pt", "element": "administrativeGenderCode", "variable": "admg" }
                              ],
                              "target": [
                                { "context": "tgt", "element": "gender", "transform": "copy", "parameter": [ { "valueId": "admg" } ] }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private String patientStructureMapUsingAliasedTargetType() {
        return """
                {
                  "resourceType": "StructureMap",
                  "structure": [
                    { "url": "http://hl7.org/cda/stds/core/StructureDefinition/RecordTarget", "mode": "source", "alias": "CDA" },
                    { "url": "http://hl7.org/fhir/StructureDefinition/Patient", "mode": "target", "alias": "FHIR" }
                  ],
                  "group": [
                    {
                      "name": "RecordTargetToPatient",
                      "input": [
                        { "name": "src", "mode": "source", "type": "CDA" },
                        { "name": "tgt", "mode": "target", "type": "FHIR" }
                      ],
                      "rule": [
                        {
                          "name": "patientRole",
                          "source": [
                            { "context": "src", "element": "patientRole", "variable": "pr" }
                          ],
                          "rule": [
                            {
                              "name": "id",
                              "source": [
                                { "context": "pr", "element": "id", "variable": "id" }
                              ],
                              "target": [
                                { "context": "tgt", "element": "identifier", "variable": "ident" }
                              ]
                            }
                          ]
                        },
                        {
                          "name": "patient",
                          "source": [
                            { "context": "patientRole", "element": "patient", "variable": "pt" }
                          ],
                          "rule": [
                            {
                              "name": "administrativeGenderCode",
                              "source": [
                                { "context": "pt", "element": "administrativeGenderCode", "variable": "admg" }
                              ],
                              "target": [
                                { "context": "tgt", "element": "gender", "transform": "copy", "parameter": [ { "valueId": "admg" } ] }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;
    }
}
