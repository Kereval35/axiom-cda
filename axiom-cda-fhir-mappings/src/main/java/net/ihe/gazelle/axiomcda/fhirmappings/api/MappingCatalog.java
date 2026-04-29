package net.ihe.gazelle.axiomcda.fhirmappings.api;

import java.util.List;
import java.util.Optional;

public interface MappingCatalog {

    List<MappingRulePack> list();

    Optional<MappingRulePack> findById(String id);

    Optional<MappingRulePack> findDefaultByRootCdaType(String rootCdaType);
}
