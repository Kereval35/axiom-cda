package net.ihe.gazelle.axiomcda.api.config;

import java.util.List;

public record TemplateSelection(List<String> classificationTypes, List<String> templateIds) {
    public TemplateSelection(List<String> classificationTypes, List<String> templateIds) {
        this.classificationTypes = classificationTypes == null ? List.of() : classificationTypes;
        this.templateIds = templateIds == null ? List.of() : templateIds;
    }
}
