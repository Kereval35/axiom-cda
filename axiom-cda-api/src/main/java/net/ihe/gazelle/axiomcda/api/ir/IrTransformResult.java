package net.ihe.gazelle.axiomcda.api.ir;

import java.util.List;

public record IrTransformResult(List<IRTemplate> templates, List<IRDiagnostic> diagnostics) {
    public IrTransformResult {
        if (templates == null) {
            throw new IllegalArgumentException("templates must be set");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("diagnostics must be set");
        }
    }
}
