package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;
import net.ihe.gazelle.axiomcda.fhirmappings.structuremap.StructureMapSemanticAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SPIFshGeneratorProviderTest {

    private final SPIFshGeneratorProvider provider = new SPIFshGeneratorProvider(List.of(
            new ObservationFshGeneratorFactory(),
            new PatientFshGeneratorFactory(),
            new GenericFshGeneratorFactory()
    ));

    @Test
    void resolvesObservationFactoryFirst() throws Exception {
        FshGeneratorFactory factory = provider.resolveFactory(new FshGeneratorContext(
                template("Observation"),
                new StructureMapSemanticAnalyzer().analyze(observationStructureMap())
        ));

        assertInstanceOf(ObservationFshGeneratorFactory.class, factory);
        assertInstanceOf(ObservationIrToFhirFshGenerator.class, factory.create());
    }

    @Test
    void resolvesPatientFactoryForPatientTargetMapping() throws Exception {
        FshGeneratorFactory factory = provider.resolveFactory(new FshGeneratorContext(
                template("RecordTarget"),
                new StructureMapSemanticAnalyzer().analyze(patientStructureMap())
        ));

        assertInstanceOf(PatientFshGeneratorFactory.class, factory);
        assertInstanceOf(PatientIrToFhirFshGenerator.class, factory.create());
    }

    @Test
    void fallsBackToGenericFactory() throws Exception {
        FshGeneratorFactory factory = provider.resolveFactory(new FshGeneratorContext(
                template("Procedure"),
                new StructureMapSemanticAnalyzer().analyze(procedureStructureMap())
        ));

        assertInstanceOf(GenericFshGeneratorFactory.class, factory);
        assertInstanceOf(GenericIrToFhirFshGenerator.class, factory.create());
    }

    private IRTemplate template(String rootType) {
        return new IRTemplate(
                "id",
                "name",
                "display",
                "description",
                rootType,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private String observationStructureMap() {
        return """
                {
                  "resourceType": "StructureMap",
                  "group": [
                    {
                      "name": "CdaLaboratoryObservationToFhirObservation",
                      "input": [
                        { "name": "src", "mode": "source", "type": "Observation" },
                        { "name": "obs", "mode": "target", "type": "Observation" }
                      ]
                    }
                  ]
                }
                """;
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
                      ]
                    }
                  ]
                }
                """;
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
                      ]
                    }
                  ]
                }
                """;
    }
}
