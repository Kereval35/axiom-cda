package net.ihe.gazelle.axiomcda.ws.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import net.ihe.gazelle.axiomcda.ws.dto.FhirPackagePreset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class FhirPackagePresetService {

    private static final Path DEFAULT_CONFIG_PATH = Path.of("/opt", "axiom-cda", "config.json");
    private static final FhirPackagePreset GENERIC_CORE_PRESET = new FhirPackagePreset(
            "HL7 FHIR R4 Core",
            "hl7.fhir.r4.core",
            "4.0.1",
            "Generic HL7 FHIR R4 core package for profiles that only depend on the base specification."
    );
    private final ObjectMapper mapper = new ObjectMapper();

    public List<FhirPackagePreset> getPresets() {
        return getPresets(configPath());
    }

    List<FhirPackagePreset> getPresets(Path configPath) {
        if (configPath == null || !Files.isRegularFile(configPath)) {
            return List.of(GENERIC_CORE_PRESET);
        }
        try {
            JsonNode root = mapper.readTree(configPath.toFile());
            JsonNode presets = root.path("fhirPackagePresets");
            if (!presets.isArray()) {
                presets = root.path("sushiPackagePresets");
            }
            if (!presets.isArray()) {
                presets = root.path("sushi").path("fhirPackagePresets");
            }
            if (!presets.isArray()) {
                return List.of(GENERIC_CORE_PRESET);
            }

            List<FhirPackagePreset> result = new ArrayList<>();
            for (JsonNode item : presets) {
                String packageId = text(item, "packageId");
                String version = text(item, "version");
                if (isBlank(packageId) || isBlank(version)) {
                    continue;
                }
                String label = text(item, "label");
                if (isBlank(label)) {
                    label = packageId + "#" + version;
                }
                result.add(new FhirPackagePreset(label, packageId, version, text(item, "description")));
            }
            appendGenericCorePreset(result);
            return List.copyOf(result);
        } catch (IOException e) {
            return List.of(GENERIC_CORE_PRESET);
        }
    }

    private void appendGenericCorePreset(List<FhirPackagePreset> presets) {
        boolean alreadyPresent = presets.stream().anyMatch(preset ->
                GENERIC_CORE_PRESET.packageId().equals(preset.packageId())
                        && GENERIC_CORE_PRESET.version().equals(preset.version())
        );
        if (!alreadyPresent) {
            presets.add(0, GENERIC_CORE_PRESET);
        }
    }

    private Path configPath() {
        String configuredPath = System.getenv("AXIOM_CDA_CONFIG");
        if (!isBlank(configuredPath)) {
            return Path.of(configuredPath);
        }
        return DEFAULT_CONFIG_PATH;
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
