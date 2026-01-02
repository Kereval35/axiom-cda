package net.ihe.gazelle.axiomcda.api.cda;

import java.util.List;

public record CdaElementDefinition(String path,
                                   List<String> typeCodes,
                                   Integer min,
                                   String max,
                                   CdaBindingStrength bindingStrength,
                                   String bindingValueSet,
                                   String fixedCode,
                                   String fixedString,
                                   Boolean fixedBoolean) {
    public CdaElementDefinition {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must be set");
        }
    }

    public boolean hasFixedValue() {
        return fixedCode != null || fixedString != null || fixedBoolean != null;
    }
}
