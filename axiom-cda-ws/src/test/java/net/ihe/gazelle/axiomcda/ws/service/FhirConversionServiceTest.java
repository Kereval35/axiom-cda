package net.ihe.gazelle.axiomcda.ws.service;

import net.ihe.gazelle.axiomcda.api.bbr.Decor;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.config.TemplateSelection;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.api.port.BbrToIrTransformer;
import net.ihe.gazelle.axiomcda.api.port.CdaModelRepository;
import net.ihe.gazelle.axiomcda.engine.business.DefaultBbrToIrTransformer;
import net.ihe.gazelle.axiomcda.engine.business.ObservationFhirConversionResult;
import net.ihe.gazelle.axiomcda.engine.business.PatientIrToFhirFshGenerator;
import net.ihe.gazelle.axiomcda.engine.technical.JaxbBbrLoader;
import net.ihe.gazelle.axiomcda.engine.technical.JsonCdaModelRepository;
import net.ihe.gazelle.axiomcda.engine.util.ResourcePaths;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;
import net.ihe.gazelle.axiomcda.fhirmappings.structuremap.StructureMapSemanticAnalyzer;
import net.ihe.gazelle.axiomcda.ws.dto.SushiCompileRequest;
import net.ihe.gazelle.axiomcda.ws.dto.SushiCompileResult;
import net.ihe.gazelle.axiomcda.ws.dto.FhirConversionRequest;
import net.ihe.gazelle.axiomcda.ws.dto.FhirConversionResult;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FhirConversionServiceTest {

    @Test
    void doesNotDuplicateR4CoreDependencyWhenSelectedAsCompilePreset() {
        FhirConversionService service = new FhirConversionService();
        SushiCompileRequest request = new SushiCompileRequest(
                "BioObservation",
                "Profile: BioObservation\nParent: http://example.org/fhir/StructureDefinition/BioObservation",
                "http://example.org/fhir/StructureDefinition/BioObservation",
                "hl7.fhir.r4.core",
                "4.0.1"
        );

        Map<String, String> dependencies = service.resolveSushiDependencies(request, false);
        String sushiConfig = service.buildSushiConfig(request, false);

        assertEquals(1, dependencies.size());
        assertEquals("4.0.1", dependencies.get("hl7.fhir.r4.core"));
        assertEquals(1, countOccurrences(sushiConfig, "hl7.fhir.r4.core: 4.0.1"));
        assertFalse(sushiConfig.contains("hl7.fhir.r4.core: 4.0.1\n  hl7.fhir.r4.core: 4.0.1"));
    }

    @Test
    void keepsCoreAndExternalDependencyWhenDifferentPackageIsSelected() {
        FhirConversionService service = new FhirConversionService();
        SushiCompileRequest request = new SushiCompileRequest(
                "BioObservation",
                "Profile: BioObservation\nParent: http://example.org/fhir/StructureDefinition/BioObservation",
                "http://example.org/fhir/StructureDefinition/BioObservation",
                "example.ig",
                "1.2.3"
        );

        Map<String, String> dependencies = service.resolveSushiDependencies(request, false);

        assertEquals(2, dependencies.size());
        assertEquals("4.0.1", dependencies.get("hl7.fhir.r4.core"));
        assertEquals("1.2.3", dependencies.get("example.ig"));
    }

    @Test
    void generatedPatientProfileFromBioRecordTargetCompilesWithSushi() throws Exception {
        URL bio = getClass().getClassLoader().getResource("observation/bio.xml");
        Path bbrPath = Path.of(bio.toURI());
        Decor decor = new JaxbBbrLoader().load(bbrPath);
        CdaModelRepository cdaRepository = new JsonCdaModelRepository(ResourcePaths.getResourcePath("package"));
        GenerationConfig config = new GenerationConfig(
                GenerationConfig.defaults().naming(),
                GenerationConfig.defaults().nullFlavorPolicy(),
                GenerationConfig.defaults().valueSetPolicy(),
                new TemplateSelection(java.util.List.of(), java.util.List.of(), true),
                GenerationConfig.defaults().emitInvariants(),
                GenerationConfig.defaults().emitIrSnapshot()
        );

        BbrToIrTransformer transformer = new DefaultBbrToIrTransformer();
        IRTemplate template = transformer.transform(decor, config, cdaRepository).templates().stream()
                .filter(t -> "RecordTarget".equals(t.rootCdaType()) && "CI-SISRecordTarget".equals(t.name()))
                .findFirst()
                .orElseThrow();

        SemanticMappingModel model = new StructureMapSemanticAnalyzer().analyze(patientStructureMap());
        ObservationFhirConversionResult conversion = new PatientIrToFhirFshGenerator()
                .generate(template, "RecordTargetCISISRecordTarget", model);

        assertTrue(conversion.fsh().contains("Parent: http://hl7.org/fhir/StructureDefinition/Patient"));
        assertTrue(conversion.fsh().contains("* name.given"));
        assertFalse(conversion.fsh().contains("name.item.given"));
        assertFalse(conversion.fsh().contains("identifier.root"));
        assertFalse(conversion.fsh().contains("* name 0..1"));
        assertFalse(conversion.fsh().contains("* address 0..1"));

        FhirConversionService service = new FhirConversionService();
        SushiCompileResult result = service.compileWithSushi(new SushiCompileRequest(
                "RecordTargetCISISRecordTargetFhirPatient",
                conversion.fsh(),
                "http://hl7.org/fhir/StructureDefinition/Patient",
                "hl7.fhir.r4.core",
                "4.0.1"
        ));

        assertTrue(result.success(), () -> String.join("\n", result.diagnostics()));
    }

    @Test
    void convertsDemo17HeartRateWithResolvedCanonicalBindings() throws Exception {
        Path bbrPath = Path.of("..", "ignored", "demo17-20260429T094232-en-US-decor-compiled.xml").normalize();
        Decor decor = new JaxbBbrLoader().load(bbrPath);
        CdaModelRepository cdaRepository = new JsonCdaModelRepository(ResourcePaths.getResourcePath("package"));
        GenerationConfig defaults = GenerationConfig.defaults();
        GenerationConfig config = new GenerationConfig(
                defaults.naming(),
                defaults.nullFlavorPolicy(),
                defaults.valueSetPolicy(),
                new TemplateSelection(java.util.List.of(), java.util.List.of("2.16.840.1.113883.3.1937.99.60.17.10.4001"), true),
                defaults.emitInvariants(),
                defaults.emitIrSnapshot()
        );

        BbrToIrTransformer transformer = new DefaultBbrToIrTransformer();
        IRTemplate template = transformer.transform(decor, config, cdaRepository).templates().stream()
                .filter(t -> "2.16.840.1.113883.3.1937.99.60.17.10.4001".equals(t.id()))
                .findFirst()
                .orElseThrow();

        FhirConversionService service = new FhirConversionService();
        FhirConversionResult result = service.convertObservation(
                new FhirConversionRequest("ObservationHeartRate", template, null, null)
        );

        String fsh = result.profiles().getFirst().content();
        assertTrue(fsh.contains("* code from http://loinc.org (required)"), fsh);
        assertTrue(fsh.contains("* interpretation from http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation (required)"), fsh);
        assertFalse(fsh.contains("* code from urn:oid:2.16.840.1.113883.3.1937.99.60.17.11.7"), fsh);
        assertFalse(fsh.contains("* interpretation from urn:oid:2.16.840.1.113883.1.11.78"), fsh);
    }

    private int countOccurrences(String content, String needle) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = content.indexOf(needle, fromIndex)) >= 0) {
            count++;
            fromIndex += needle.length();
        }
        return count;
    }

    private String patientStructureMap() {
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
