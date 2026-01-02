package net.ihe.gazelle.axiomcda.api.ir;

public record IRCardinality(int min, String max) {
    public IRCardinality {
        if (min < 0) {
            throw new IllegalArgumentException("min must be >= 0");
        }
        if (max == null || max.isBlank()) {
            throw new IllegalArgumentException("max must be set");
        }
    }

    public String format() {
        return min + ".." + max;
    }
}
