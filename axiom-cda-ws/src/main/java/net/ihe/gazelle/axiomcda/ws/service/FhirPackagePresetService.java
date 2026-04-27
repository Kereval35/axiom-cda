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
    private final ObjectMapper mapper = new ObjectMapper();

    public List<FhirPackagePreset> getPresets() {
        return getPresets(configPath());
    }

    List<FhirPackagePreset> getPresets(Path configPath) {
        if (configPath == null || !Files.isRegularFile(configPath)) {
            return List.of();
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
                return List.of();
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
            return List.copyOf(result);
        } catch (IOException e) {
            return List.of();
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
