package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.ir.IRBinding;
import net.ihe.gazelle.axiomcda.api.ir.IRBindingStrength;
import net.ihe.gazelle.axiomcda.api.ir.IRCardinality;
import net.ihe.gazelle.axiomcda.api.ir.IRFixedValueType;
import net.ihe.gazelle.axiomcda.api.ir.IRElementConstraint;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;
import net.ihe.gazelle.axiomcda.fhirmappings.structuremap.StructureMapSemanticAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericIrToFhirFshGeneratorTest {

    @Test
    void generatesGenericFhirProfileFromUploadedStructureMap() throws Exception {
        IRTemplate template = new IRTemplate(
                "proc-template",
                "Procedure Activity",
                "Procedure Activity",
                "Procedure Activity",
                "Procedure",
                List.of(
                        new IRElementConstraint("id", new IRCardinality(1, "*"), null, null, null, List.of(), null),
                        new IRElementConstraint("code", new IRCardinality(1, "1"), null, null, null,
                                List.of(new IRBinding(IRBindingStrength.REQUIRED, "http://example.org/fhir/ValueSet/procedure-code", null)), "Procedure code"),
                        new IRElementConstraint("code.code", null, null, "80146002", IRFixedValueType.CODE, List.of(), null),
                        new IRElementConstraint("statusCode", new IRCardinality(1, "1"), null, null, null, List.of(), null),
                        new IRElementConstraint("statusCode.code", null, null, "completed", IRFixedValueType.CODE, List.of(), null)
                ),
                List.of(),
                List.of()
        );
        SemanticMappingModel model = new StructureMapSemanticAnalyzer().analyze(procedureStructureMap());

        ObservationFhirConversionResult result = new GenericIrToFhirFshGenerator()
                .generate(template, "ProcedureActivity", model);

        assertTrue(result.fsh().contains("Parent: http://hl7.org/fhir/StructureDefinition/Procedure"));
        assertTrue(result.fsh().contains("Profile: ProcedureActivityFhirProcedure"));
        assertTrue(result.fsh().contains("* identifier 1..*"));
        assertTrue(result.fsh().contains("* code 1..1"));
        assertTrue(result.fsh().contains("* code from http://example.org/fhir/ValueSet/procedure-code (required)"));
        assertTrue(result.fsh().contains("* code ^short = \"Procedure code\""));
        assertTrue(result.fsh().contains("* code.coding.code = #80146002"));
        assertTrue(result.fsh().contains("* status 1..1"));
        assertTrue(result.fsh().contains("* status = #completed"));
        assertFalse(result.usedMappingModel().allRules().isEmpty());
    }

    private String procedureStructureMap() {
        return """
                {
                  "resourceType": "StructureMap",
                  "group": [
                    {
                      "name": "CdaProcedureToFhirProcedure",
                      "input": [
                        { "name": "src", "mode": "source", "type": "Procedure" },
                        { "name": "procedure", "mode": "target", "type": "Procedure" }
                      ],
                      "rule": [
                        {
                          "name": "parent",
                          "target": [
                            {
                              "context": "procedure",
                              "element": "meta.profile",
                              "transform": "copy",
                              "parameter": [
                                { "valueString": "http://hl7.org/fhir/StructureDefinition/Procedure" }
                              ]
                            }
                          ]
                        },
                        {
                          "name": "id",
                          "source": [
                            { "context": "src", "element": "id" }
                          ],
                          "target": [
                            { "context": "procedure", "element": "identifier" }
                          ]
                        },
                        {
                          "name": "code",
                          "source": [
                            { "context": "src", "element": "code" }
                          ],
                          "target": [
                            { "context": "procedure", "element": "code" }
                          ],
                          "rule": [
                            {
                              "name": "codeCode",
                              "source": [
                                { "context": "src", "element": "code.code" }
                              ],
                              "target": [
                                { "context": "procedure", "element": "code.coding.code" }
                              ]
                            }
                          ]
                        },
                        {
                          "name": "status",
                          "source": [
                            { "context": "src", "element": "statusCode" }
                          ],
                          "target": [
                            { "context": "procedure", "element": "status" }
                          ],
                          "rule": [
                            {
                              "name": "statusCode",
                              "source": [
                                { "context": "src", "element": "statusCode.code" }
                              ],
                              "target": [
                                { "context": "procedure", "element": "status" }
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
