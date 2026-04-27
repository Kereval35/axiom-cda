package net.ihe.gazelle.axiomcda.ws.dto;

import java.util.List;

public record SushiCompileResult(
        boolean success,
        String structureDefinitionJson,
        List<String> diagnostics,
        String sushiConfig,
        String generatedFileName
) {
    public SushiCompileResult {
        if (diagnostics == null) {
            diagnostics = List.of();
        }
    }
}
