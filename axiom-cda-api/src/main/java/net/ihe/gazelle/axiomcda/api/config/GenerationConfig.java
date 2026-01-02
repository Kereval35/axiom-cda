package net.ihe.gazelle.axiomcda.api.config;

import net.ihe.gazelle.axiomcda.api.ir.IRBindingStrength;

import java.util.List;
import java.util.Map;

public record GenerationConfig(
        NamingConfig naming,
        NullFlavorPolicy nullFlavorPolicy,
        ValueSetPolicy valueSetPolicy,
        TemplateSelection templateSelection,
        boolean emitInvariants,
        boolean emitIrSnapshot
) {
    public GenerationConfig(NamingConfig naming,
                            NullFlavorPolicy nullFlavorPolicy,
                            ValueSetPolicy valueSetPolicy,
                            TemplateSelection templateSelection,
                            boolean emitInvariants,
                            boolean emitIrSnapshot) {
        this.naming = naming == null ? new NamingConfig(null, null, null, null, null) : naming;
        this.nullFlavorPolicy = nullFlavorPolicy == null ? new NullFlavorPolicy(null) : nullFlavorPolicy;
        this.valueSetPolicy = valueSetPolicy == null ? new ValueSetPolicy(null, null, true) : valueSetPolicy;
        this.templateSelection = templateSelection == null ? new TemplateSelection(null, null) : templateSelection;
        this.emitInvariants = emitInvariants;
        this.emitIrSnapshot = emitIrSnapshot;
    }

    public static GenerationConfig defaults() {
        return new GenerationConfig(
                new NamingConfig("", "", "", Map.of(), Map.of()),
                new NullFlavorPolicy(List.of()),
                new ValueSetPolicy(Map.of(), IRBindingStrength.EXTENSIBLE, true),
                new TemplateSelection(List.of(), List.of()),
                true,
                false
        );
    }
}
