package net.ihe.gazelle.axiomcda.fhirmappings.trace;

import net.ihe.gazelle.axiomcda.fhirmappings.api.DependentCallNode;
import net.ihe.gazelle.axiomcda.fhirmappings.api.MappingKind;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticGroup;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticRule;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SourceNode;
import net.ihe.gazelle.axiomcda.fhirmappings.api.TargetNode;
import net.ihe.gazelle.axiomcda.fhirmappings.api.TargetParameter;

import java.util.ArrayList;
import java.util.List;

public class SemanticMappingFshTraceRenderer {

    public String render(String artifactName,
                         String rootCdaType,
                         String sourceDescription,
                         SemanticMappingModel model) {
        List<String> lines = new ArrayList<>();
        lines.add("RuleSet: " + safeIdentifier(artifactName));
        lines.add("* metadata.rootCdaType = " + quoted(rootCdaType));
        lines.add("* metadata.mappingSource = " + quoted(sourceDescription));
        lines.add("* metadata.note = " + quoted("Human-readable trace of the effective CDA to FHIR mapping used for this conversion."));
        lines.add("* metadata.purpose = " + quoted("Documentation artifact for user review, audit, and qualification."));

        if (model == null || model.groups().isEmpty()) {
            lines.add("");
            lines.add("* metadata.empty = true");
            return String.join("\n", lines) + "\n";
        }

        int mappingIndex = 0;
        for (SemanticGroup group : model.groups()) {
            for (SemanticRule rule : group.rules()) {
                mappingIndex = appendRule(lines, mappingIndex, group.name(), rule);
            }
        }
        return String.join("\n", lines) + "\n";
    }

    private int appendRule(List<String> lines, int mappingIndex, String groupName, SemanticRule rule) {
        List<SourceNode> sources = rule.sources().isEmpty()
                ? List.of(new SourceNode(rule.primarySourcePath(), null, null, null, rule.conditional()))
                : rule.sources();
        List<TargetNode> targets = rule.targets().isEmpty()
                ? List.of(new TargetNode(null, null, null, null, null, List.of(), rule.conditional()))
                : rule.targets();

        for (SourceNode source : sources) {
            for (TargetNode target : targets) {
                appendMapping(lines, mappingIndex++, groupName, rule, source, target);
            }
        }
        for (SemanticRule child : rule.children()) {
            mappingIndex = appendRule(lines, mappingIndex, groupName, child);
        }
        return mappingIndex;
    }

    private void appendMapping(List<String> lines,
                               int mappingIndex,
                               String groupName,
                               SemanticRule rule,
                               SourceNode source,
                               TargetNode target) {
        String mappingPath = "mapping[" + mappingIndex + "]";
        lines.add("");
        String sourceLabel = displaySource(rule, source);
        String targetLabel = displayTarget(target);
        lines.add("// " + sourceLabel + " -> " + targetLabel);
        lines.add("* ruleName = " + quoted(rule.name()));
        lines.add("* groupName = " + quoted(groupName));
        lines.add("* mappingKind = #" + (rule.mappingKind() == null ? MappingKind.CONTEXT_DERIVED.name() : rule.mappingKind().name()));
        lines.add("* from = " + quoted(sourceLabel));
        lines.add("* to = " + quoted(targetLabel));
        lines.add("* conditional = " + (rule.conditional() || source.conditional() || target.conditional()));

        if (source.type() != null) {
            lines.add("* sourceType = " + quoted(source.type()));
        }
        if (source.variable() != null) {
            lines.add("* sourceVariable = " + quoted(source.variable()));
        }
        if (source.condition() != null) {
            lines.add("* condition = " + quoted(source.condition()));
        }
        if (target.variable() != null) {
            lines.add("* targetVariable = " + quoted(target.variable()));
        }
        if (target.transform() != null && !target.transform().isBlank()) {
            lines.add("* transform = " + quoted(target.transform()));
        }
        if (target.constantValue() != null) {
            lines.add("* constantValue = " + quoted(target.constantValue()));
        }
        if (target.createdType() != null) {
            lines.add("* createdType = " + quoted(target.createdType()));
        }
        for (TargetParameter parameter : target.parameters()) {
            lines.add("* parameter." + safeSegment(parameter.kind()) + " = " + quoted(parameter.value()));
        }
        for (DependentCallNode dependentCall : rule.dependentCalls()) {
            lines.add("* helper = " + quoted(dependentCall.name()));
        }
    }

    private String displaySource(SemanticRule rule, SourceNode source) {
        if (rule.displaySourceLabel() != null && !rule.displaySourceLabel().isBlank()
                && (source.path() == null || source.path().isBlank())) {
            return rule.displaySourceLabel();
        }
        if (source.path() != null && !source.path().isBlank()) {
            return source.path();
        }
        return rule.displaySourceLabel() == null || rule.displaySourceLabel().isBlank()
                ? "CONTEXT"
                : rule.displaySourceLabel();
    }

    private String displayTarget(TargetNode target) {
        return target.path() == null || target.path().isBlank() ? "HELPER" : target.path();
    }

    private String quoted(String value) {
        return "\"" + escape(safe(value)) + "\"";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "<none>" : value;
    }

    private String safePath(String value) {
        return value == null || value.isBlank() ? "<none>" : value;
    }

    private String safeIdentifier(String value) {
        String safe = safe(value).replaceAll("[^A-Za-z0-9]", "");
        return safe.isBlank() ? "GeneratedMappingRules" : safe;
    }

    private String safeSegment(String value) {
        String safe = safe(value).replaceAll("[^A-Za-z0-9]", "");
        return safe.isBlank() ? "value" : Character.toLowerCase(safe.charAt(0)) + safe.substring(1);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
