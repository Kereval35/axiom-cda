package net.ihe.gazelle.axiomcda.ws.service;

import net.ihe.gazelle.axiomcda.ws.dto.SushiCompileRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

    private int countOccurrences(String content, String needle) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = content.indexOf(needle, fromIndex)) >= 0) {
            count++;
            fromIndex += needle.length();
        }
        return count;
    }
}
