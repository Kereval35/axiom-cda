package net.ihe.gazelle.axiomcda.engine.business;

import java.util.Set;

record OwnershipContext(String projectId,
                        String projectPrefix,
                        Set<String> templateRoots,
                        Set<String> scenarioTemplateRefs) {
}
