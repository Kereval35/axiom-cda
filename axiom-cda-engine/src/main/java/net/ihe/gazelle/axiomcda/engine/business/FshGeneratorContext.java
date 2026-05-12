package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;

public record FshGeneratorContext(
        IRTemplate template,
        SemanticMappingModel mappingModel
) {
}
