package net.ihe.gazelle.axiomcda.api.cda;

import java.util.Map;

public record CdaStructureDefinition(String name, String url, Map<String, CdaElementDefinition> elementsByPath) {
    public CdaStructureDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must be set");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url must be set");
        }
        if (elementsByPath == null) {
            throw new IllegalArgumentException("elementsByPath must be set");
        }
    }
}
