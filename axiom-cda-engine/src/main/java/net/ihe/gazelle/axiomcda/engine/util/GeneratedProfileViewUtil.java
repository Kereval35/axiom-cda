package net.ihe.gazelle.axiomcda.engine.util;

import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.fsh.FshBundle;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplateOrigin;
import net.ihe.gazelle.axiomcda.engine.business.DefaultIrToFshGenerator;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GeneratedProfileViewUtil {

    private GeneratedProfileViewUtil() {
    }

    public static int countVisibleProfiles(FshBundle bundle, List<IRTemplate> templates, GenerationConfig config) {
        return visibleProfileNames(bundle, templates, config).size();
    }

    public static Set<String> visibleProfileNames(FshBundle bundle, List<IRTemplate> templates, GenerationConfig config) {
        Set<String> candidateNames = configuredVisibleProfileNames(templates, config);
        Set<String> visibleNames = new LinkedHashSet<>();
        String resourcesDir = DefaultIrToFshGenerator.RESOURCES_DIR + "/";
        for (String filePath : bundle.files().keySet()) {
            if (!filePath.startsWith(resourcesDir)) {
                continue;
            }
            String fileName = filePath.substring(resourcesDir.length());
            String profileName = fileName.endsWith(".fsh") ? fileName.substring(0, fileName.length() - 4) : fileName;
            if (candidateNames.contains(profileName)) {
                visibleNames.add(profileName);
            }
        }
        return visibleNames;
    }

    private static Set<String> configuredVisibleProfileNames(List<IRTemplate> templates, GenerationConfig config) {
        Map<String, String> profileNames = ProfileNamingUtil.resolveProfileNames(templates, config);
        Set<String> visibleNames = new LinkedHashSet<>();
        boolean projectOnly = config != null
                && config.templateSelection() != null
                && config.templateSelection().projectPlusRequiredIncludes();
        for (IRTemplate template : templates) {
            if (template == null) {
                continue;
            }
            if (projectOnly && template.origin() != IRTemplateOrigin.PROJECT) {
                continue;
            }
            String profileName = profileNames.get(template.id());
            if (profileName != null && !profileName.isBlank()) {
                visibleNames.add(profileName);
            }
        }
        return visibleNames;
    }
}
