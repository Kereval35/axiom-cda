package net.ihe.gazelle.axiomcda.ws.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GenerationRequest(
        String bbr, // Can be XML content or URL
        boolean sushiRepo,
        boolean emitIr,
        boolean emitLogs,
        String yamlConfig,
        boolean projectPlusRequiredIncludes,
        List<String> ownedRepositoryPrefixes
) {
    public GenerationRequest() {
        this(null, true, false, true, null, false, List.of());
    }
}
