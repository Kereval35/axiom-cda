package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.cda.CdaStructureDefinition;
import net.ihe.gazelle.axiomcda.api.port.CdaModelRepository;
import net.ihe.gazelle.axiomcda.engine.technical.JsonCdaModelRepository;
import net.ihe.gazelle.axiomcda.engine.util.ResourcePaths;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CdaPathNormalizerTest {
    @Test
    void resolvesStructuredBodyComponentAttributesFromRootDefinition() throws Exception {
        CdaModelRepository repository = new JsonCdaModelRepository(ResourcePaths.getResourcePath("package"));
        CdaStructureDefinition clinicalDocument = repository.findByName("ClinicalDocument").orElseThrow();

        assertEquals("component.structuredBody.component.typeCode",
                CdaPathNormalizer.normalizePath("component.structuredBody.component.typeCode",
                        clinicalDocument,
                        repository));
        assertEquals("component.structuredBody.component.contextConductionInd",
                CdaPathNormalizer.normalizePath("component.structuredBody.component.contextConductionInd",
                        clinicalDocument,
                        repository));
    }

    @Test
    void stillResolvesRegularTypedTransitions() throws Exception {
        CdaModelRepository repository = new JsonCdaModelRepository(ResourcePaths.getResourcePath("package"));
        CdaStructureDefinition clinicalDocument = repository.findByName("ClinicalDocument").orElseThrow();

        assertEquals("recordTarget.patientRole",
                CdaPathNormalizer.normalizePath("recordTarget.patientRole", clinicalDocument, repository));
    }
}
