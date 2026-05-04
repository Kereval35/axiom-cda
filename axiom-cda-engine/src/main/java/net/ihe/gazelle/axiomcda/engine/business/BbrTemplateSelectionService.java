package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.bbr.BaseId;
import net.ihe.gazelle.axiomcda.api.bbr.Decor;
import net.ihe.gazelle.axiomcda.api.bbr.DecorObjectType;
import net.ihe.gazelle.axiomcda.api.bbr.IncludeDefinition;
import net.ihe.gazelle.axiomcda.api.bbr.RuleDefinition;
import net.ihe.gazelle.axiomcda.api.bbr.Scenario;
import net.ihe.gazelle.axiomcda.api.bbr.Rules;
import net.ihe.gazelle.axiomcda.api.bbr.TemplateDefinition;
import net.ihe.gazelle.axiomcda.api.bbr.TemplateProperties;
import net.ihe.gazelle.axiomcda.api.bbr.TemplateStatusCodeLifeCycle;
import net.ihe.gazelle.axiomcda.api.bbr.Transaction;
import net.ihe.gazelle.axiomcda.api.config.TemplateSelection;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplateOrigin;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

class BbrTemplateSelectionService {
    private static final List<String> DEFAULT_OWNED_REPOSITORY_PREFIXES = List.of("bbr-");

    Map<String, TemplateDefinition> selectLatestById(Rules rules) {
        Map<String, List<TemplateDefinition>> byId = new HashMap<>();
        for (Object entry : rules.getTemplateAssociationOrTemplate()) {
            if (entry instanceof TemplateDefinition template) {
                if (!isActive(template)) {
                    continue;
                }
                if (template.getId() == null || template.getId().isBlank()) {
                    continue;
                }
                byId.computeIfAbsent(template.getId(), key -> new ArrayList<>()).add(template);
            }
        }

        Map<String, TemplateDefinition> latestById = new LinkedHashMap<>();
        for (Map.Entry<String, List<TemplateDefinition>> entry : byId.entrySet()) {
            TemplateDefinition selected = entry.getValue().stream()
                    .max(Comparator.comparing(this::effectiveInstant))
                    .orElse(null);
            if (selected != null) {
                latestById.put(entry.getKey(), selected);
            }
        }
        return latestById;
    }

    Set<String> selectTemplateIds(Map<String, TemplateDefinition> latestById, TemplateSelection selection) {
        Set<String> selected = new LinkedHashSet<>();
        for (TemplateDefinition template : latestById.values()) {
            if (matchesSelection(template, selection)) {
                selected.add(template.getId());
            }
        }
        if (selection != null && !selection.templateIds().isEmpty()) {
            selected.addAll(selection.templateIds());
        }
        return selected;
    }

    Set<String> findProjectOwnedIds(Set<String> selectedIds,
                                    Map<String, TemplateDefinition> latestById,
                                    OwnershipContext ownershipContext,
                                    TemplateSelection selection) {
        Set<String> owned = new LinkedHashSet<>();
        for (String templateId : selectedIds) {
            TemplateDefinition template = latestById.get(templateId);
            if (template != null && isProjectOwned(template, ownershipContext, selection)) {
                owned.add(templateId);
            }
        }
        return owned;
    }

    Set<String> expandIncludes(Set<String> seedIds, Map<String, TemplateDefinition> latestById) {
        Set<String> expanded = new LinkedHashSet<>(seedIds);
        Deque<String> queue = new ArrayDeque<>(seedIds);
        while (!queue.isEmpty()) {
            String templateId = queue.removeFirst();
            TemplateDefinition template = latestById.get(templateId);
            if (template == null) {
                continue;
            }
            for (String includeId : collectIncludes(template)) {
                if (expanded.add(includeId)) {
                    queue.add(includeId);
                }
            }
        }
        return expanded;
    }

    Map<String, IRTemplateOrigin> classifyOrigins(Set<String> projectOwnedIds,
                                                   Set<String> expandedIds,
                                                   Map<String, TemplateDefinition> latestById,
                                                   OwnershipContext ownershipContext,
                                                   TemplateSelection selection) {
        Map<String, IRTemplateOrigin> origins = new LinkedHashMap<>();
        if (selection == null || !selection.projectPlusRequiredIncludes()) {
            for (String templateId : expandedIds) {
                TemplateDefinition template = latestById.get(templateId);
                if (template == null) {
                    continue;
                }
                origins.put(templateId, isProjectOwned(template, ownershipContext, selection)
                        ? IRTemplateOrigin.PROJECT
                        : IRTemplateOrigin.OTHER);
            }
            return origins;
        }

        for (String templateId : expandedIds) {
            TemplateDefinition template = latestById.get(templateId);
            if (template == null) {
                continue;
            }
            if (projectOwnedIds.contains(templateId)) {
                origins.put(templateId, IRTemplateOrigin.PROJECT);
            } else {
                origins.put(templateId, IRTemplateOrigin.REQUIRED_INCLUDE);
            }
        }
        return origins;
    }

    OwnershipContext ownershipContextFromDecor(Decor decor) {
        String projectId = decor != null && decor.getProject() != null ? decor.getProject().getId() : null;
        String projectPrefix = decor != null && decor.getProject() != null ? normalizeIdent(decor.getProject().getPrefix()) : null;
        Set<String> templateRoots = new LinkedHashSet<>();
        if (decor != null && decor.getIds() != null) {
            for (BaseId baseId : decor.getIds().getBaseId()) {
                if (baseId.getType() == DecorObjectType.TM && baseId.getId() != null && !baseId.getId().isBlank()) {
                    templateRoots.add(baseId.getId());
                }
            }
        }
        Set<String> scenarioTemplateRefs = new LinkedHashSet<>();
        if (decor != null && decor.getScenarios() != null) {
            for (Scenario scenario : decor.getScenarios().getScenario()) {
                collectRepresentingTemplateRefs(scenario.getTransaction(), scenarioTemplateRefs);
            }
        }
        return new OwnershipContext(projectId, projectPrefix, templateRoots, scenarioTemplateRefs);
    }

    private boolean matchesSelection(TemplateDefinition template, TemplateSelection selection) {
        if (selection == null) {
            return true;
        }
        if (!selection.templateIds().isEmpty()) {
            return selection.templateIds().contains(template.getId());
        }
        if (selection.classificationTypes().isEmpty()) {
            return true;
        }
        for (TemplateProperties properties : template.getClassification()) {
            if (properties.getType() != null && selection.classificationTypes().contains(properties.getType().value())) {
                return true;
            }
        }
        return false;
    }

    private boolean isActive(TemplateDefinition template) {
        TemplateStatusCodeLifeCycle status = template.getStatusCode();
        if (status == null) {
            return true;
        }
        return status != TemplateStatusCodeLifeCycle.RETIRED
                && status != TemplateStatusCodeLifeCycle.CANCELLED
                && status != TemplateStatusCodeLifeCycle.REJECTED;
    }

    private Instant effectiveInstant(TemplateDefinition template) {
        XMLGregorianCalendar date = template.getEffectiveDate();
        if (date == null) {
            return Instant.EPOCH;
        }
        return date.toGregorianCalendar().toInstant();
    }

    private boolean isProjectOwned(TemplateDefinition template, OwnershipContext ownershipContext, TemplateSelection selection) {
        if (template == null || ownershipContext == null) {
            return false;
        }
        String templateId = template.getId();
        if (templateId == null || templateId.isBlank()) {
            return false;
        }
        if (ownershipContext.projectId() != null
                && (templateId.equals(ownershipContext.projectId()) || templateId.startsWith(ownershipContext.projectId() + "."))) {
            return true;
        }
        for (String templateRoot : ownershipContext.templateRoots()) {
            if (templateId.equals(templateRoot) || templateId.startsWith(templateRoot + ".")) {
                return true;
            }
        }
        if (ownershipContext.scenarioTemplateRefs().contains(templateId)) {
            return true;
        }
        String templateIdent = normalizeRepositoryIdent(template.getIdent());
        String referencedFrom = normalizeRepositoryIdent(template.getReferencedFrom());
        Set<String> ownedRepositoryPrefixes = ownedRepositoryPrefixes(ownershipContext, selection);
        if (ownershipContext.projectPrefix() != null
                && (ownershipContext.projectPrefix().equals(templateIdent)
                || ownershipContext.projectPrefix().equals(referencedFrom))) {
            return true;
        }
        if (templateIdent != null && ownedRepositoryPrefixes.contains(templateIdent)) {
            return true;
        }
        if (referencedFrom != null && ownedRepositoryPrefixes.contains(referencedFrom)) {
            return true;
        }
        if (referencedFrom != null && !referencedFrom.isBlank() && ownershipContext.projectPrefix() != null
                && !ownershipContext.projectPrefix().equals(referencedFrom)) {
            return false;
        }
        if (templateIdent == null || templateIdent.isBlank()) {
            return template.getReferencedFrom() == null || template.getReferencedFrom().isBlank();
        }
        return false;
    }

    private Set<String> ownedRepositoryPrefixes(OwnershipContext ownershipContext, TemplateSelection selection) {
        Set<String> ownedPrefixes = new LinkedHashSet<>(DEFAULT_OWNED_REPOSITORY_PREFIXES);
        if (ownershipContext.projectPrefix() != null) {
            ownedPrefixes.add(ownershipContext.projectPrefix());
        }
        if (selection != null) {
            for (String prefix : selection.ownedRepositoryPrefixes()) {
                String normalized = normalizeRepositoryIdent(prefix);
                if (normalized != null) {
                    ownedPrefixes.add(normalized);
                }
            }
        }
        return ownedPrefixes;
    }

    private String normalizeRepositoryIdent(String ident) {
        if (ident == null) {
            return null;
        }
        String normalized = ident.trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private void collectRepresentingTemplateRefs(List<Transaction> transactions, Set<String> refs) {
        if (transactions == null) {
            return;
        }
        for (Transaction transaction : transactions) {
            if (transaction.getRepresentingTemplate() != null) {
                String ref = transaction.getRepresentingTemplate().getRef();
                if (ref != null && !ref.isBlank()) {
                    refs.add(ref);
                }
            }
            collectRepresentingTemplateRefs(transaction.getTransactions(), refs);
        }
    }

    private List<String> collectIncludes(TemplateDefinition template) {
        List<String> includes = new ArrayList<>();
        if (template.getAttributeOrChoiceOrElement() == null) {
            return includes;
        }
        for (Object entry : template.getAttributeOrChoiceOrElement()) {
            if (entry instanceof IncludeDefinition include) {
                if (include.getRef() != null && !include.getRef().isBlank()) {
                    includes.add(include.getRef());
                }
            } else if (entry instanceof RuleDefinition rule) {
                collectIncludes(rule, includes);
            }
        }
        return includes;
    }

    private void collectIncludes(RuleDefinition rule, List<String> includes) {
        if (rule.getLetOrAssertOrReport() == null) {
            return;
        }
        for (Object entry : rule.getLetOrAssertOrReport()) {
            if (entry instanceof IncludeDefinition include) {
                if (include.getRef() != null && !include.getRef().isBlank()) {
                    includes.add(include.getRef());
                }
            } else if (entry instanceof RuleDefinition childRule) {
                collectIncludes(childRule, includes);
            }
        }
    }

    private String normalizeIdent(String ident) {
        if (ident == null) {
            return null;
        }
        String normalized = ident.trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
