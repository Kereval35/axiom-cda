package net.ihe.gazelle.axiomcda.ws.service;

import jakarta.enterprise.context.ApplicationScoped;
import net.ihe.gazelle.axiomcda.fhirmappings.api.MappingRulePack;
import net.ihe.gazelle.axiomcda.fhirmappings.builtin.BuiltInMappingCatalog;
import net.ihe.gazelle.axiomcda.ws.dto.FhirBuiltInMappingPreset;

import java.util.List;

@ApplicationScoped
public class FhirBuiltInMappingPresetService {

    private final BuiltInMappingCatalog catalog = new BuiltInMappingCatalog();

    public List<FhirBuiltInMappingPreset> getObservationPresets() {
        List<MappingRulePack> packs = catalog.list().stream()
                .filter(pack -> "Observation".equals(pack.rootCdaType()))
                .toList();
        if (packs.isEmpty()) {
            return List.of();
        }
        String defaultId = packs.get(0).id();
        return packs.stream()
                .map(pack -> new FhirBuiltInMappingPreset(
                        pack.id(),
                        pack.label(),
                        pack.description(),
                        pack.rootCdaType(),
                        pack.family(),
                        pack.id().equals(defaultId)
                ))
                .toList();
    }
}
