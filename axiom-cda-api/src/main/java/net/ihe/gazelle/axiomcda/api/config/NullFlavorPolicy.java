package net.ihe.gazelle.axiomcda.api.config;

import java.util.List;

public record NullFlavorPolicy(List<String> forbiddenPaths) {
    public NullFlavorPolicy(List<String> forbiddenPaths) {
        this.forbiddenPaths = forbiddenPaths == null ? List.of() : forbiddenPaths;
    }
}
