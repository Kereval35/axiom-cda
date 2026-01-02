package net.ihe.gazelle.axiomcda.api.fsh;

import java.util.Map;

public record FshBundle(Map<String, String> files) {
    public FshBundle {
        if (files == null) {
            throw new IllegalArgumentException("files must be set");
        }
    }
}
