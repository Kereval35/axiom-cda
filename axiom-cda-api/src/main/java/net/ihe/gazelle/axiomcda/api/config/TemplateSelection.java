package net.ihe.gazelle.axiomcda.api.config;

import java.util.List;

public record TemplateSelection(List<String> classificationTypes,
                                List<String> templateIds,
                                boolean projectPlusRequiredIncludes,
                                List<String> ownedRepositoryPrefixes) {
    public TemplateSelection(List<String> classificationTypes, List<String> templateIds) {
        this(classificationTypes, templateIds, false, List.of());
    }

    public TemplateSelection(List<String> classificationTypes,
                             List<String> templateIds,
                             boolean projectPlusRequiredIncludes) {
        this(classificationTypes, templateIds, projectPlusRequiredIncludes, List.of());
    }

    public TemplateSelection(List<String> classificationTypes,
                             List<String> templateIds,
                             boolean projectPlusRequiredIncludes,
                             List<String> ownedRepositoryPrefixes) {
        this.classificationTypes = classificationTypes == null ? List.of() : classificationTypes;
        this.templateIds = templateIds == null ? List.of() : templateIds;
        this.projectPlusRequiredIncludes = projectPlusRequiredIncludes;
        this.ownedRepositoryPrefixes = ownedRepositoryPrefixes == null ? List.of() : ownedRepositoryPrefixes;
    }
}
