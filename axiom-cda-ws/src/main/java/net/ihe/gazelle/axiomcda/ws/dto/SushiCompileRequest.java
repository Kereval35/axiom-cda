package net.ihe.gazelle.axiomcda.ws.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SushiCompileRequest(
        String profileName,
        String fshContent,
        String parent,
        String dependencyPackageId,
        String dependencyVersion
) {
}
