package net.ihe.gazelle.axiomcda.api.ir;

import java.util.List;

public record IRElementConstraint(
        String path,
        IRCardinality cardinality,
        String datatype,
        String fixedValue,
        IRFixedValueType fixedValueType,
        List<IRBinding> bindings,
        String shortDescription
) {
    public IRElementConstraint {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must be set");
        }
    }
}
