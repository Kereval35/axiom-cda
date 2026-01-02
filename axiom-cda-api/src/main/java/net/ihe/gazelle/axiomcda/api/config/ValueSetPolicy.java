package net.ihe.gazelle.axiomcda.api.config;

import net.ihe.gazelle.axiomcda.api.ir.IRBindingStrength;

import java.util.Map;

public record ValueSetPolicy(
        Map<String, String> oidToCanonical,
        IRBindingStrength defaultStrength,
        boolean useOidAsCanonical
) {
    public ValueSetPolicy(Map<String, String> oidToCanonical,
                          IRBindingStrength defaultStrength,
                          boolean useOidAsCanonical) {
        this.oidToCanonical = oidToCanonical == null ? Map.of() : oidToCanonical;
        this.defaultStrength = defaultStrength == null ? IRBindingStrength.EXTENSIBLE : defaultStrength;
        this.useOidAsCanonical = useOidAsCanonical;
    }
}
