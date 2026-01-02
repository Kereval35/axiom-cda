package net.ihe.gazelle.axiomcda.engine.util;

public final class FshUtil {
    private FshUtil() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
