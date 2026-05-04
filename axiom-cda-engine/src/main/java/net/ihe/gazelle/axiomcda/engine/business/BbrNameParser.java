package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.engine.util.NameUtil;

final class BbrNameParser {
    private BbrNameParser() {
    }

    static ParsedName parseElementName(String rawName) {
        if (rawName == null) {
            return null;
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String namePart = trimmed;
        int bracket = trimmed.indexOf('[');
        String predicate = null;
        if (bracket >= 0) {
            int end = trimmed.lastIndexOf(']');
            predicate = end > bracket ? trimmed.substring(bracket + 1, end) : trimmed.substring(bracket + 1);
            namePart = trimmed.substring(0, bracket);
        }
        int colon = namePart.lastIndexOf(':');
        String prefix = colon >= 0 ? namePart.substring(0, colon) : null;
        String localName = colon >= 0 ? namePart.substring(colon + 1) : namePart;
        if (localName.startsWith("@")) {
            localName = localName.substring(1);
        }
        String baseName = localName;
        if ("sdtc".equalsIgnoreCase(prefix)) {
            baseName = "sdtc" + NameUtil.upperFirst(localName);
        }
        return new ParsedName(baseName, predicate);
    }

    static String normalizeName(String rawName) {
        ParsedName parsed = parseElementName(rawName);
        return parsed == null ? rawName : parsed.baseName();
    }

    static String resolveRootCdaType(String baseName) {
        if (baseName == null) {
            return null;
        }
        return switch (baseName) {
            case "assignedPerson" -> "Person";
            case "assignedAuthoringDevice" -> "AuthoringDevice";
            case "representedOrganization" -> "Organization";
            case "representedCustodianOrganization" -> "CustodianOrganization";
            case "addr" -> "AD";
            case "telecom" -> "TEL";
            case "name" -> "PN";
            case "id" -> "II";
            case "participant" -> "Participant1";
            case "performer" -> "Performer1";
            default -> NameUtil.upperFirst(baseName);
        };
    }

    static String normalizeInvariantPath(String rawPath, String rootType) {
        String path = rawPath.trim();
        path = path.replaceAll("^//", "");
        path = path.replaceAll("^\\/\\/", "");
        path = path.replaceAll("^\\*:\\/?", "");
        path = path.replaceAll("\\[.*?\\]", "");
        path = path.replace("hl7:", "");
        path = path.replace("*:", "");
        path = path.replace("/", ".");
        path = path.replace("@", "");
        path = path.replaceAll("^\\.", "");
        if (rootType != null && path.startsWith(rootType + ".")) {
            path = path.substring(rootType.length() + 1);
        }
        return path;
    }
}
