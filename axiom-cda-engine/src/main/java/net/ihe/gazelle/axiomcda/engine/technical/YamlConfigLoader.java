package net.ihe.gazelle.axiomcda.engine.technical;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class YamlConfigLoader {
    public GenerationConfig load(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path must be set");
        }
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Config file not found: " + path);
        }
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(path.toFile(), GenerationConfig.class);
    }
}
