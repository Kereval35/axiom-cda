package net.ihe.gazelle.axiomcda.fhirmappings.structuremap;

import net.ihe.gazelle.axiomcda.fhirmappings.api.MappingModelProvider;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;

public class StructureMapUploadModelProvider implements MappingModelProvider {

    private final String structureMapJson;

    public StructureMapUploadModelProvider(String structureMapJson) {
        this.structureMapJson = structureMapJson;
    }

    @Override
    public SemanticMappingModel resolve(String rootCdaType) throws Exception {
        if (structureMapJson == null || structureMapJson.isBlank()) {
            throw new IllegalArgumentException("structureMapJson must be provided");
        }
        return new StructureMapSemanticAnalyzer().analyze(structureMapJson);
    }
}
