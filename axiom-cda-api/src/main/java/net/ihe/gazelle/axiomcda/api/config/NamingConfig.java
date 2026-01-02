package net.ihe.gazelle.axiomcda.api.config;

import java.util.Map;

public record NamingConfig(
        String profilePrefix,
        String idPrefix,
        String titlePrefix,
        Map<String, String> profileNameOverrides,
        Map<String, String> idOverrides
) {
    public NamingConfig(String profilePrefix,
                        String idPrefix,
                        String titlePrefix,
                        Map<String, String> profileNameOverrides,
                        Map<String, String> idOverrides) {
        this.profilePrefix = profilePrefix == null ? "" : profilePrefix;
        this.idPrefix = idPrefix == null ? "" : idPrefix;
        this.titlePrefix = titlePrefix == null ? "" : titlePrefix;
        this.profileNameOverrides = profileNameOverrides == null ? Map.of() : profileNameOverrides;
        this.idOverrides = idOverrides == null ? Map.of() : idOverrides;
    }
}
