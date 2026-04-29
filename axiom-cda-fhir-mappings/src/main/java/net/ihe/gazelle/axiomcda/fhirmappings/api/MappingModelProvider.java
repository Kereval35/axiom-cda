package net.ihe.gazelle.axiomcda.fhirmappings.api;

public interface MappingModelProvider {

    SemanticMappingModel resolve(String rootCdaType) throws Exception;
}
