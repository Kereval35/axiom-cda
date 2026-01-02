package net.ihe.gazelle.axiomcda.engine.technical;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ihe.gazelle.axiomcda.api.cda.CdaBindingStrength;
import net.ihe.gazelle.axiomcda.api.cda.CdaElementDefinition;
import net.ihe.gazelle.axiomcda.api.cda.CdaStructureDefinition;
import net.ihe.gazelle.axiomcda.api.port.CdaModelRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class JsonCdaModelRepository implements CdaModelRepository {
    private final Map<String, CdaStructureDefinition> byName;

    public JsonCdaModelRepository(Path packageDir) throws IOException {
        if (packageDir == null) {
            throw new IllegalArgumentException("packageDir must be set");
        }
        if (!Files.isDirectory(packageDir)) {
            throw new IllegalArgumentException("CDA package directory not found: " + packageDir);
        }
        this.byName = loadPackage(packageDir);
    }

    @Override
    public Optional<CdaStructureDefinition> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        CdaStructureDefinition definition = byName.get(name.toLowerCase(Locale.ROOT));
        return Optional.ofNullable(definition);
    }

    private Map<String, CdaStructureDefinition> loadPackage(Path packageDir) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, CdaStructureDefinition> map = new HashMap<>();
        try (var stream = Files.list(packageDir)) {
            for (Path file : stream.filter(path -> path.getFileName().toString().startsWith("StructureDefinition-")
                    && path.getFileName().toString().endsWith(".json")).toList()) {
                CdaStructureDefinitionJson json = mapper.readValue(file.toFile(), CdaStructureDefinitionJson.class);
                if (json.name == null || json.url == null || json.snapshot == null || json.snapshot.element == null) {
                    continue;
                }
                Map<String, CdaElementDefinition> elements = new HashMap<>();
                for (CdaElementJson element : json.snapshot.element) {
                    if (element.path == null || element.path.isBlank()) {
                        continue;
                    }
                    List<String> typeCodes = element.type == null
                            ? List.of()
                            : element.type.stream()
                                    .map(type -> type.code)
                                    .filter(code -> code != null && !code.isBlank())
                                    .toList();
                    CdaBindingStrength bindingStrength = mapBindingStrength(element.binding);
                    String bindingValueSet = element.binding != null ? element.binding.valueSet : null;
                    elements.put(element.path, new CdaElementDefinition(
                            element.path,
                            typeCodes,
                            element.min,
                            element.max,
                            bindingStrength,
                            bindingValueSet,
                            element.fixedCode,
                            element.fixedString,
                            element.fixedBoolean
                    ));
                }
                CdaStructureDefinition definition = new CdaStructureDefinition(json.name, json.url, elements);
                map.put(json.name.toLowerCase(Locale.ROOT), definition);
            }
        }
        return map;
    }

    private CdaBindingStrength mapBindingStrength(CdaBindingJson binding) {
        if (binding == null || binding.strength == null) {
            return null;
        }
        return switch (binding.strength) {
            case "required" -> CdaBindingStrength.REQUIRED;
            case "extensible" -> CdaBindingStrength.EXTENSIBLE;
            case "preferred", "example" -> CdaBindingStrength.PREFERRED;
            default -> null;
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CdaStructureDefinitionJson {
        public String name;
        public String url;
        public Snapshot snapshot;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Snapshot {
        public List<CdaElementJson> element;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CdaElementJson {
        public String path;
        public List<CdaTypeJson> type;
        public Integer min;
        public String max;
        public String fixedCode;
        public String fixedString;
        public Boolean fixedBoolean;
        public CdaBindingJson binding;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CdaTypeJson {
        public String code;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CdaBindingJson {
        public String strength;
        public String valueSet;
    }
}
