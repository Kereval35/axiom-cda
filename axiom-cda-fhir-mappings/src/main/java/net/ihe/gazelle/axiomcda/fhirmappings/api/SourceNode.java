package net.ihe.gazelle.axiomcda.fhirmappings.api;

public record SourceNode(String path,
                         String type,
                         String variable,
                         String condition,
                         boolean conditional) {
}
