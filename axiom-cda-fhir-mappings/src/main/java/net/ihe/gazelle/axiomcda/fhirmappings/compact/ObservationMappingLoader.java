package net.ihe.gazelle.axiomcda.fhirmappings.compact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;

public final class ObservationMappingLoader {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public ObservationMapping load(InputStream stream) throws IOException {
        if (stream == null) {
            throw new IllegalArgumentException("stream must be set");
        }
        return YAML_MAPPER.readValue(stream, ObservationMapping.class);
    }
}
