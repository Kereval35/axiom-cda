package net.ihe.gazelle.axiomcda.fhirmappings.builtin;

import net.ihe.gazelle.axiomcda.fhirmappings.api.MappingCatalog;
import net.ihe.gazelle.axiomcda.fhirmappings.api.MappingModelProvider;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;
import net.ihe.gazelle.axiomcda.fhirmappings.api.UnknownBuiltInMappingException;

public class BuiltInMappingModelProvider implements MappingModelProvider {

    private final MappingCatalog catalog;

    public BuiltInMappingModelProvider() {
        this(new BuiltInMappingCatalog());
    }

    public BuiltInMappingModelProvider(MappingCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public SemanticMappingModel resolve(String rootCdaType) {
        return catalog.findDefaultByRootCdaType(rootCdaType)
                .map(pack -> pack.model())
                .orElseThrow(() -> new UnknownBuiltInMappingException(
                        "No built-in mapping pack is available for CDA root type '" + rootCdaType + "'."
                ));
    }

    public SemanticMappingModel resolve(String rootCdaType, String mappingId) {
        if (mappingId == null || mappingId.isBlank()) {
            return resolve(rootCdaType);
        }
        return catalog.findById(mappingId)
                .filter(pack -> rootCdaType != null && rootCdaType.equals(pack.rootCdaType()))
                .map(pack -> pack.model())
                .orElseThrow(() -> new UnknownBuiltInMappingException(
                        "No built-in mapping pack '" + mappingId + "' is available for CDA root type '" + rootCdaType + "'."
                ));
    }
}
