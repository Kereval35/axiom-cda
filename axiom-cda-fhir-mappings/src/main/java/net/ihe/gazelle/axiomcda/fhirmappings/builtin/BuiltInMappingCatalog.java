package net.ihe.gazelle.axiomcda.fhirmappings.builtin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.ihe.gazelle.axiomcda.fhirmappings.api.MappingCatalog;
import net.ihe.gazelle.axiomcda.fhirmappings.api.MappingRulePack;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModelEnricher;
import net.ihe.gazelle.axiomcda.fhirmappings.compact.ObservationMapping;
import net.ihe.gazelle.axiomcda.fhirmappings.compact.ObservationMappingCompiler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class BuiltInMappingCatalog implements MappingCatalog {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private final List<MappingRulePack> packs;

    public BuiltInMappingCatalog() {
        this.packs = loadPacks();
    }

    @Override
    public List<MappingRulePack> list() {
        return packs;
    }

    @Override
    public Optional<MappingRulePack> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return packs.stream()
                .filter(pack -> pack.id() != null)
                .filter(pack -> pack.id().trim().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }

    @Override
    public Optional<MappingRulePack> findDefaultByRootCdaType(String rootCdaType) {
        if (rootCdaType == null || rootCdaType.isBlank()) {
            return Optional.empty();
        }
        String normalized = rootCdaType.trim().toLowerCase(Locale.ROOT);
        return packs.stream()
                .filter(pack -> pack.rootCdaType() != null)
                .filter(pack -> pack.rootCdaType().trim().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }

    private List<MappingRulePack> loadPacks() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("fhir-mappings/index.yaml")) {
            if (stream == null) {
                throw new IllegalStateException("Missing built-in mapping index");
            }
            MappingIndex index = YAML_MAPPER.readValue(stream, MappingIndex.class);
            List<MappingRulePack> loaded = new ArrayList<>();
            for (MappingIndexEntry entry : index.packs()) {
                loaded.add(new MappingRulePack(
                        entry.id(),
                        entry.label(),
                        entry.description(),
                        entry.rootCdaType(),
                        entry.fhirVersion(),
                        entry.family(),
                        entry.version(),
                        entry.status(),
                        loadModel(entry.resource())
                ));
            }
            return List.copyOf(loaded);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load built-in mapping catalog", e);
        }
    }

    private SemanticMappingModel loadModel(String resource) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Missing mapping resource: " + resource);
            }
            JsonNode root = YAML_MAPPER.readTree(stream);
            if (root == null || root.isMissingNode() || root.isNull()) {
                return new SemanticMappingModel(List.of());
            }
            String kind = root.path("kind").asText("");
            if ("observation-mapping".equals(kind)) {
                ObservationMapping mapping = YAML_MAPPER.treeToValue(root, ObservationMapping.class);
                return new ObservationMappingCompiler().compile(mapping);
            }
            return SemanticMappingModelEnricher.enrich(YAML_MAPPER.treeToValue(root, SemanticMappingModel.class));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MappingIndex(List<MappingIndexEntry> packs) {
        private MappingIndex {
            packs = packs == null ? List.of() : List.copyOf(packs);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MappingIndexEntry(String id,
                                     String label,
                                     String description,
                                     String rootCdaType,
                                     String fhirVersion,
                                     String family,
                                     String version,
                                     String status,
                                     String resource) {
    }
}
