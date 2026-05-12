package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.engine.util.FshUtil;
import net.ihe.gazelle.axiomcda.engine.util.NameUtil;

import java.util.LinkedHashSet;

public final class FhirProfileSupport {

    private FhirProfileSupport() {
    }

    public static String buildProfileName(String sourceProfileName, IRTemplate template, String resourceName) {
        String base = (sourceProfileName == null || sourceProfileName.isBlank())
                ? template.rootCdaType()
                : sourceProfileName;
        String sanitized = base.replaceAll("[^A-Za-z0-9]", "");
        String resource = resourceName == null || resourceName.isBlank() ? "Resource" : resourceName;
        return sanitized + "Fhir" + resource;
    }

    public static String buildProfileId(String profileName) {
        return NameUtil.toKebabCase(profileName);
    }

    public static LinkedHashSet<String> initializeProfileLines(String profileName,
                                                               String parent,
                                                               String description) {
        LinkedHashSet<String> lines = new LinkedHashSet<>();
        lines.add("Profile: " + profileName);
        lines.add("Parent: " + parent);
        lines.add("Id: " + buildProfileId(profileName));
        lines.add("Title: \"" + FshUtil.escape(profileName) + "\"");
        lines.add("Description: \"" + FshUtil.escape(description) + "\"");
        lines.add("* ^status = #draft");
        return lines;
    }
}
