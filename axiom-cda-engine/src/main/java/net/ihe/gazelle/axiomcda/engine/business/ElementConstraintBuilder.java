package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.ir.IRBinding;
import net.ihe.gazelle.axiomcda.api.ir.IRCardinality;
import net.ihe.gazelle.axiomcda.api.ir.IRDiagnosticSeverity;
import net.ihe.gazelle.axiomcda.api.ir.IRElementConstraint;
import net.ihe.gazelle.axiomcda.api.ir.IRFixedValueType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ElementConstraintBuilder {
    private final String path;
    private IRCardinality cardinality;
    private String datatype;
    private String fixedValue;
    private IRFixedValueType fixedValueType;
    private List<IRBinding> bindings;
    private String shortDescription;

    ElementConstraintBuilder(String path) {
        this.path = path;
    }

    void applyCardinality(IRCardinality newCardinality, TemplateBuildContext context) {
        if (this.cardinality != null && !this.cardinality.equals(newCardinality)) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, path, "Conflicting cardinalities");
            return;
        }
        this.cardinality = newCardinality;
    }

    void applyDatatype(QName datatype) {
        if (datatype == null) {
            return;
        }
        this.datatype = datatype.getLocalPart();
    }

    void applyFixedValue(String value, QName datatype, String attributeName) {
        if (value == null) {
            return;
        }
        if (this.fixedValue != null && !this.fixedValue.equals(value)) {
            return;
        }
        this.fixedValue = value;
        this.fixedValueType = determineFixedValueType(datatype, attributeName);
    }

    void applyBindings(List<IRBinding> newBindings, TemplateBuildContext context) {
        if (newBindings.isEmpty()) {
            return;
        }
        if (bindings == null) {
            bindings = new ArrayList<>();
        }
        if (!bindings.isEmpty()) {
            context.addDiagnostic(IRDiagnosticSeverity.WARNING, path, "Multiple bindings found; keeping first");
            return;
        }
        bindings.addAll(newBindings);
    }

    void applyShortDescription(String description) {
        if (description == null || description.isBlank()) {
            return;
        }
        if (shortDescription == null) {
            shortDescription = description;
        }
    }

    IRElementConstraint build() {
        return new IRElementConstraint(path, cardinality, datatype, fixedValue, fixedValueType,
                bindings == null ? List.of() : List.copyOf(bindings), shortDescription);
    }

    private IRFixedValueType determineFixedValueType(QName datatype, String attributeName) {
        String data = datatype != null ? datatype.getLocalPart() : null;
        String name = attributeName == null ? "" : attributeName;
        if (name.endsWith("Ind") || name.endsWith("Indicator")) {
            return IRFixedValueType.BOOLEAN;
        }
        if (data != null) {
            String upper = data.toUpperCase(Locale.ROOT);
            if (upper.equals("BL") || upper.equals("BOOLEAN") || upper.equals("BOOL") || upper.startsWith("BL.")) {
                return IRFixedValueType.BOOLEAN;
            }
            if (upper.equals("CS") || upper.equals("CE") || upper.equals("CD") || upper.equals("CV")) {
                return IRFixedValueType.CODE;
            }
        }
        if (name.endsWith("Code") || name.equals("code") || name.equals("classCode") || name.equals("moodCode") || name.equals("typeCode")) {
            return IRFixedValueType.CODE;
        }
        return IRFixedValueType.STRING;
    }
}
