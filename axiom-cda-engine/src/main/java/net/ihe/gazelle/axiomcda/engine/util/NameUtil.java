package net.ihe.gazelle.axiomcda.engine.util;

import java.text.Normalizer;

public final class NameUtil {
    private NameUtil() {
    }

    public static String stripPrefix(String name) {
        if (name == null) {
            return null;
        }
        int colon = name.lastIndexOf(':');
        if (colon >= 0 && colon + 1 < name.length()) {
            return name.substring(colon + 1);
        }
        return name;
    }

    public static String lowerFirst(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    public static String upperFirst(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public static String toKebabCase(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        char prev = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '_' || c == ' ' || c == '-') {
                if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '-') {
                    builder.append('-');
                }
                prev = c;
                continue;
            }
            if (Character.isUpperCase(c) && i > 0 && prev != '-' && prev != '_' && prev != ' ' && !Character.isUpperCase(prev)) {
                builder.append('-');
            }
            builder.append(Character.toLowerCase(c));
            prev = c;
        }
        return builder.toString().replaceAll("-+", "-");
    }

    public static String toPascalCase(String value) {
        if (value == null) {
            return null;
        }
        String normalized = normalizeAscii(value);
        String[] parts = normalized.split("[^A-Za-z0-9]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String clean = part.replaceAll("[^A-Za-z0-9]", "");
            if (clean.isEmpty()) {
                continue;
            }
            if (Character.isDigit(clean.charAt(0))) {
                builder.append('T');
            }
            builder.append(upperFirst(clean.substring(0, 1).toLowerCase() + clean.substring(1)));
        }
        return builder.toString();
    }

    public static String normalizeAscii(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }
}
