package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;

import java.io.IOException;

public interface IrToFhirFshGenerator {

    ObservationFhirConversionResult generate(IRTemplate template,
                                             String sourceProfileName,
                                             SemanticMappingModel semanticModel) throws IOException;
}
