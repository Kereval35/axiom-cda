package net.ihe.gazelle.axiomcda.ws.service;

import net.ihe.gazelle.axiomcda.ws.dto.FhirPackagePreset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.Files.writeString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FhirPackagePresetServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void readsConfiguredFhirPackagePresets() throws Exception {
        Path config = tempDir.resolve("config.json");
        writeString(config, """
                {
                  "fhirPackagePresets": [
                    {
                      "label": "MyHealth EU Laboratory",
                      "packageId": "myhealth.eu.fhir.laboratory",
                      "version": "0.1.1",
                      "description": "eHDSI laboratory implementation guide"
                    },
                    {
                      "label": "Incomplete",
                      "packageId": "missing.version"
                    }
                  ]
                }
                """, StandardCharsets.UTF_8);

        List<FhirPackagePreset> presets = new FhirPackagePresetService().getPresets(config);

        assertEquals(1, presets.size());
        assertEquals("MyHealth EU Laboratory", presets.get(0).label());
        assertEquals("myhealth.eu.fhir.laboratory", presets.get(0).packageId());
        assertEquals("0.1.1", presets.get(0).version());
        assertEquals("eHDSI laboratory implementation guide", presets.get(0).description());
    }

    @Test
    void returnsEmptyListWhenConfigIsMissing() {
        assertTrue(new FhirPackagePresetService().getPresets(tempDir.resolve("missing.json")).isEmpty());
    }
}
