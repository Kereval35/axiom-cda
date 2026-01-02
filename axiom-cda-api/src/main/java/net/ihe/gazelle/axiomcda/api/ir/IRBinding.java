package net.ihe.gazelle.axiomcda.api.ir;

public record IRBinding(IRBindingStrength strength, String valueSetRef, String codeSystemRef) {
    public IRBinding {
        if (strength == null) {
            throw new IllegalArgumentException("strength must be set");
        }
    }
}
