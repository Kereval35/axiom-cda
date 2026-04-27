package net.ihe.gazelle.axiomcda.ws.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FhirConversionRequest(
        String sourceProfileName,
        IRTemplate template,
        String structureMap
) {
}
