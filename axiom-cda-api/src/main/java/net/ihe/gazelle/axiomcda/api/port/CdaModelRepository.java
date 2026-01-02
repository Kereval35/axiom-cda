package net.ihe.gazelle.axiomcda.api.port;

import net.ihe.gazelle.axiomcda.api.cda.CdaStructureDefinition;

import java.util.Optional;

public interface CdaModelRepository {
    Optional<CdaStructureDefinition> findByName(String name);
}
