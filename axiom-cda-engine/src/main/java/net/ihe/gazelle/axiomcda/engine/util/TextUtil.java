package net.ihe.gazelle.axiomcda.engine.util;

import net.ihe.gazelle.axiomcda.api.bbr.FreeFormMarkupWithLanguage;
import org.w3c.dom.Element;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class TextUtil {
    private TextUtil() {
    }

    public static String selectDescription(List<FreeFormMarkupWithLanguage> descs, String preferredLanguage) {
        if (descs == null || descs.isEmpty()) {
            return null;
        }
        FreeFormMarkupWithLanguage selected = descs.stream()
                .filter(desc -> preferredLanguage != null && preferredLanguage.equalsIgnoreCase(desc.getLanguage()))
                .findFirst()
                .orElseGet(() -> descs.stream()
                        .filter(desc -> desc.getLanguage() != null)
                        .min(Comparator.comparing(FreeFormMarkupWithLanguage::getLanguage))
                        .orElse(descs.get(0))
                );
        return flattenMarkup(selected);
    }

    public static String flattenMarkup(FreeFormMarkupWithLanguage markup) {
        if (markup == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (Object part : markup.getContent()) {
            if (part instanceof String text) {
                builder.append(text);
            } else if (part instanceof Element element) {
                builder.append(element.getTextContent());
            }
        }
        String raw = builder.toString();
        String cleaned = raw.replaceAll("\\s+", " ").trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    public static String flattenMixedContent(List<Object> content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (Object part : content) {
            if (part instanceof String text) {
                builder.append(text);
            } else if (part instanceof Element element) {
                builder.append(element.getTextContent());
            }
        }
        return normalizeWhitespace(builder.toString());
    }

    public static String normalizeWhitespace(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("\\s+", " ").trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    public static String safeString(String value) {
        return Objects.requireNonNullElse(value, "");
    }
}
