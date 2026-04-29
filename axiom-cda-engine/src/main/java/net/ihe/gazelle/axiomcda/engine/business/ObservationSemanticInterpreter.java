package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.ir.*;
import net.ihe.gazelle.axiomcda.engine.util.FshUtil;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticMappingModel;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SemanticRule;
import net.ihe.gazelle.axiomcda.fhirmappings.api.SourceNode;
import net.ihe.gazelle.axiomcda.fhirmappings.api.TargetNode;

import java.util.*;

class ObservationSemanticInterpreter {

    private static final String DEFAULT_PARENT = "http://hl7.org/fhir/StructureDefinition/Observation";
    private static final Set<String> IGNORED_CDA_ROOTS = Set.of(
            "classCode",
            "moodCode",
            "negationInd",
            "templateId",
            "priorityCode",
            "repeatNumber",
            "languageCode",
            "derivationExpr"
    );
    private static final Set<String> IGNORED_ENTRY_RELATIONSHIP_EXACT_PATHS = Set.of(
            "entryRelationship.typeCode",
            "entryRelationship.inversionInd",
            "entryRelationship.act.classCode",
            "entryRelationship.act.moodCode",
            "entryRelationship.observation.classCode",
            "entryRelationship.observation.moodCode"
    );
    private static final List<String> IGNORED_ENTRY_RELATIONSHIP_PREFIXES = List.of(
            "entryRelationship.act.templateId",
            "entryRelationship.observation.templateId"
    );
    private enum EntryRelationshipFlavor {
        NOTE,
        HAS_MEMBER,
        AMBIGUOUS,
        NONE
    }

    ObservationProjectionResult interpret(IRTemplate template,
                                          SemanticMappingModel model,
                                          List<BranchInferenceEngine.BranchInference> inferences) {
        String parent = resolveParent(model);
        LinkedHashSet<ProjectionCandidate> candidates = new LinkedHashSet<>();
        List<ProjectionDiagnostic> diagnostics = new ArrayList<>();
        Set<SemanticRule> usedRules = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<String, BranchInferenceEngine.BranchInference> inferenceByBranch = new LinkedHashMap<>();
        for (BranchInferenceEngine.BranchInference inference : inferences) {
            inferenceByBranch.put(inference.sourceBranch(), inference);
        }

        seedGlobalConstants(model, candidates, usedRules);
        markParentRuleUsed(model, usedRules, parent);
        EntryRelationshipFlavor entryRelationshipFlavor = deriveEntryRelationshipFlavor(template, model);

        Set<String> reportedDiagnosticKeys = new HashSet<>();
        for (IRElementConstraint element : template.elements()) {
            String path = element.path();
            String root = rootSegment(path);
            if (root == null || IGNORED_CDA_ROOTS.contains(root)) {
                continue;
            }
            BranchInferenceEngine.BranchInference inference = inferenceByBranch.get(root);
            switch (root) {
                case "id" -> {
                    markRulesUsedForBranch(model, usedRules, "id", Set.of("identifier"), element);
                    if (!emitDirectBranch(element, "identifier", inference, candidates, diagnostics, reportedDiagnosticKeys, false)) {
                        recordDiagnostic(root, path, "unsupported_branch", "No safe FHIR path mapping found for CDA branch '" + root + "'.", BranchInferenceEngine.BranchConfidence.UNSAFE, diagnostics, reportedDiagnosticKeys);
                    }
                }
                case "code" -> {
                    markRulesUsedForBranch(model, usedRules, "code", Set.of("code"), element);
                    emitCodeBranch(element, inference, candidates, diagnostics, reportedDiagnosticKeys);
                }
                case "statusCode" -> {
                    markRulesUsedForBranch(model, usedRules, "statusCode", Set.of("status"), element);
                    emitStatusBranch(element, inference, candidates, diagnostics, reportedDiagnosticKeys);
                }
                case "effectiveTime" -> {
                    markRulesUsedForBranch(model, usedRules, "effectiveTime", Set.of("effective"), element);
                    emitEffectiveBranch(element, inference, candidates, diagnostics, reportedDiagnosticKeys);
                }
                case "text" -> emitTextBranch(element, candidates);
                case "value" -> {
                    markRulesUsedForBranch(model, usedRules, "value", Set.of("value"), element);
                    emitValueBranch(element, inference, candidates, diagnostics, reportedDiagnosticKeys);
                }
                case "interpretationCode" -> {
                    markRulesUsedForBranch(model, usedRules, "interpretationCode", Set.of("interpretation"), element);
                    emitCodeableConceptBranch(element, inference, "interpretation", candidates, diagnostics, reportedDiagnosticKeys);
                }
                case "methodCode" -> {
                    markRulesUsedForBranch(model, usedRules, "methodCode", Set.of("method"), element);
                    emitMethodBranch(element, inference, candidates, diagnostics, reportedDiagnosticKeys);
                }
                case "participant" -> {
                    markRulesUsedForBranch(model, usedRules, "participant", Set.of("performer"), element);
                    emitReferenceBranch(element, inference, "performer", candidates, diagnostics, reportedDiagnosticKeys);
                }
                case "performer" -> {
                    markRulesUsedForBranch(model, usedRules, "participant", Set.of("performer"), element);
                    emitPerformerBranch(element, inference, inferenceByBranch.get("participant"), candidates, diagnostics, reportedDiagnosticKeys);
                }
                case "entryRelationship" -> {
                    markRulesUsedForEntryRelationship(model, usedRules, element);
                    emitEntryRelationshipBranch(element, inference, model, entryRelationshipFlavor, candidates, diagnostics, reportedDiagnosticKeys);
                }
                case "reference" -> {
                    markRulesUsedForBranch(model, usedRules, "reference", Set.of("derivedFrom", "hasMember", "partOf", "note"), element);
                    emitReferenceLinkBranch(element, inference, candidates, diagnostics, reportedDiagnosticKeys);
                }
                case "referenceRange" -> {
                    markRulesUsedForBranch(model, usedRules, "referenceRange", Set.of("referenceRange"), element);
                    emitReferenceRangeBranch(element, inference, candidates, diagnostics, reportedDiagnosticKeys);
                }
                case "author" -> {
                    if (!isAuthorAbsorbedByNote(inferenceByBranch.get("entryRelationship"))) {
                        recordDiagnostic(root, path, "runtime_only_reference_creation",
                                "CDA branch '" + root + "' participates in runtime reference construction and is not emitted as a standalone profile rule.",
                                inference != null ? inference.confidence() : BranchInferenceEngine.BranchConfidence.RUNTIME_ONLY,
                                diagnostics,
                                reportedDiagnosticKeys);
                    }
                }
                default -> {
                    if (inference == null || inference.confidence() == BranchInferenceEngine.BranchConfidence.RUNTIME_ONLY) {
                        recordDiagnostic(root, path, "unsupported_branch",
                                "No safe FHIR path mapping found for CDA branch '" + root + "'.",
                                inference != null ? inference.confidence() : BranchInferenceEngine.BranchConfidence.UNSAFE,
                                diagnostics,
                                reportedDiagnosticKeys);
                    }
                }
            }
        }
        return new ObservationProjectionResult(parent, List.copyOf(candidates), List.copyOf(diagnostics), Set.copyOf(usedRules));
    }

    private void seedGlobalConstants(SemanticMappingModel model,
                                     Set<ProjectionCandidate> candidates,
                                     Set<SemanticRule> usedRules) {
        for (SemanticRule rule : model.allRules()) {
            for (TargetNode target : rule.targets()) {
                if (target.path() == null || target.path().isBlank() || target.conditional()) {
                    continue;
                }
                if ("category.coding.system".equals(target.path()) || "category.coding.code".equals(target.path())) {
                    if (target.constantValue() != null) {
                        usedRules.add(rule);
                        candidates.add(ProjectionCandidate.fixed(target.path(), target.constantValue(), defaultFixedTypeForPath(target.path())));
                    }
                }
            }
        }
    }

    private String resolveParent(SemanticMappingModel model) {
        for (SemanticRule rule : model.allRules()) {
            for (TargetNode target : rule.targets()) {
                if ("meta.profile".equals(target.path()) && target.constantValue() != null) {
                    return target.constantValue();
                }
            }
        }
        return DEFAULT_PARENT;
    }

    private void markParentRuleUsed(SemanticMappingModel model,
                                    Set<SemanticRule> usedRules,
                                    String parent) {
        for (SemanticRule rule : model.allRules()) {
            for (TargetNode target : rule.targets()) {
                if ("meta.profile".equals(target.path()) && Objects.equals(parent, target.constantValue())) {
                    usedRules.add(rule);
                }
            }
        }
    }

    private void markRulesUsedForBranch(SemanticMappingModel model,
                                        Set<SemanticRule> usedRules,
                                        String sourceRoot,
                                        Set<String> targetRoots,
                                        IRElementConstraint element) {
        String datatype = element.datatype() == null ? null : element.datatype().replace('-', '_');
        for (SemanticRule rule : model.allRules()) {
            if (!matchesSourceRoot(rule, sourceRoot)) {
                continue;
            }
            if (!matchesDatatype(rule, datatype)) {
                continue;
            }
            if (!matchesTargetRoots(rule, targetRoots)) {
                continue;
            }
            usedRules.add(rule);
        }
    }

    private void markRulesUsedForEntryRelationship(SemanticMappingModel model,
                                                   Set<SemanticRule> usedRules,
                                                   IRElementConstraint element) {
        String targetRoot = entryRelationshipTargetRoot(element.path(), model, EntryRelationshipFlavor.NONE);
        if (targetRoot == null) {
            return;
        }
        for (SemanticRule rule : model.allRules()) {
            if (!matchesSourcePath(rule, element.path())) {
                continue;
            }
            if (!matchesTargetRoots(rule, Set.of(targetRoot))) {
                continue;
            }
            usedRules.add(rule);
        }
    }

    private boolean matchesSourceRoot(SemanticRule rule, String sourceRoot) {
        if (sourceRoot == null || sourceRoot.isBlank()) {
            return false;
        }
        if (rule.primarySourcePath() != null && sourceRoot.equals(rootSegment(rule.primarySourcePath()))) {
            return true;
        }
        if (!rule.branchLineage().isEmpty() && sourceRoot.equals(rule.branchLineage().get(rule.branchLineage().size() - 1))) {
            return true;
        }
        return rule.sources().stream()
                .map(SourceNode::path)
                .filter(Objects::nonNull)
                .map(this::rootSegment)
                .anyMatch(sourceRoot::equals);
    }

    private boolean matchesDatatype(SemanticRule rule, String datatype) {
        List<String> ruleTypes = rule.sources().stream()
                .map(SourceNode::type)
                .filter(Objects::nonNull)
                .filter(type -> !type.isBlank())
                .toList();
        return ruleTypes.isEmpty() || datatype == null || ruleTypes.contains(datatype);
    }

    private boolean matchesTargetRoots(SemanticRule rule, Set<String> targetRoots) {
        if (targetRoots == null || targetRoots.isEmpty()) {
            return true;
        }
        Set<String> ruleTargetRoots = rule.targets().stream()
                .map(TargetNode::path)
                .filter(Objects::nonNull)
                .filter(path -> !path.isBlank())
                .map(this::rootSegment)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        return ruleTargetRoots.isEmpty() || ruleTargetRoots.stream().anyMatch(targetRoots::contains);
    }

    private boolean matchesSourcePath(SemanticRule rule, String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return false;
        }
        if (pathMatches(rule.primarySourcePath(), sourcePath)) {
            return true;
        }
        return rule.sources().stream()
                .map(SourceNode::path)
                .anyMatch(path -> pathMatches(path, sourcePath));
    }

    private boolean pathMatches(String rulePath, String sourcePath) {
        if (rulePath == null || rulePath.isBlank() || sourcePath == null || sourcePath.isBlank()) {
            return false;
        }
        return rulePath.equals(sourcePath)
                || sourcePath.startsWith(rulePath + ".")
                || rulePath.startsWith(sourcePath + ".");
    }

    private void emitCodeBranch(IRElementConstraint element,
                                BranchInferenceEngine.BranchInference inference,
                                Set<ProjectionCandidate> candidates,
                                List<ProjectionDiagnostic> diagnostics,
                                Set<String> reportedDiagnosticKeys) {
        if (inference == null || !inference.targetsObservationRoot("code")) {
            recordDiagnostic("code", element.path(), "unsupported_branch",
                    "No safe FHIR path mapping found for CDA branch 'code'.",
                    inference != null ? inference.confidence() : BranchInferenceEngine.BranchConfidence.UNSAFE,
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        String targetPath = switch (element.path()) {
            case "code" -> "code";
            case "code.code" -> "code.coding.code";
            case "code.codeSystem" -> "code.coding.system";
            case "code.displayName" -> "code.coding.display";
            case "code.originalText" -> "code.text";
            case "code.translation" -> "code.coding";
            case "code.translation.code" -> "code.coding.code";
            case "code.translation.codeSystem" -> "code.coding.system";
            case "code.translation.displayName" -> "code.coding.display";
            case "code.originalText.reference" -> null;
            default -> null;
        };
        if ("code.originalText.reference".equals(element.path())) {
            return;
        }
        if (targetPath == null) {
            recordDiagnostic("code", element.path(), "branch_partially_supported",
                    "CDA branch 'code' maps to FHIR Observation.code, but path '" + element.path() + "' is not yet safely projected.",
                    inference.confidence(),
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        emitGenericRules(element, targetPath, candidates);
    }

    private void emitStatusBranch(IRElementConstraint element,
                                  BranchInferenceEngine.BranchInference inference,
                                  Set<ProjectionCandidate> candidates,
                                  List<ProjectionDiagnostic> diagnostics,
                                  Set<String> reportedDiagnosticKeys) {
        if (inference == null || !inference.targetsObservationRoot("status")) {
            recordDiagnostic("statusCode", element.path(), "unsupported_branch",
                    "No safe FHIR path mapping found for CDA branch 'statusCode'.",
                    inference != null ? inference.confidence() : BranchInferenceEngine.BranchConfidence.UNSAFE,
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        String targetPath = "status";
        emitGenericRules(element, targetPath, candidates);
    }

    private void emitEffectiveBranch(IRElementConstraint element,
                                     BranchInferenceEngine.BranchInference inference,
                                     Set<ProjectionCandidate> candidates,
                                     List<ProjectionDiagnostic> diagnostics,
                                     Set<String> reportedDiagnosticKeys) {
        if (inference == null || !inference.targetsObservationRoot("effective")) {
            recordDiagnostic("effectiveTime", element.path(), "unsupported_branch",
                    "No safe FHIR path mapping found for CDA branch 'effectiveTime'.",
                    inference != null ? inference.confidence() : BranchInferenceEngine.BranchConfidence.UNSAFE,
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        if (element.cardinality() != null) {
            candidates.add(ProjectionCandidate.cardinality("effective[x]", element.cardinality()));
        }
        candidates.add(ProjectionCandidate.onlyType("effective[x]", "dateTime"));
        if (element.shortDescription() != null && !element.shortDescription().isBlank()) {
            candidates.add(ProjectionCandidate.shortDescription("effective[x]", element.shortDescription()));
        }
    }

    private void emitTextBranch(IRElementConstraint element,
                                Set<ProjectionCandidate> candidates) {
        if ("text".equals(element.path())) {
            if (element.cardinality() != null) {
                candidates.add(ProjectionCandidate.cardinality("note", element.cardinality()));
            }
            if (element.fixedValue() != null) {
                IRFixedValueType type = element.fixedValueType() == null ? IRFixedValueType.STRING : element.fixedValueType();
                candidates.add(ProjectionCandidate.fixed("note.text", element.fixedValue(), type));
            }
            if (element.shortDescription() != null && !element.shortDescription().isBlank()) {
                candidates.add(ProjectionCandidate.shortDescription("note.text", element.shortDescription()));
            }
        }
    }

    private void emitValueBranch(IRElementConstraint element,
                                 BranchInferenceEngine.BranchInference inference,
                                 Set<ProjectionCandidate> candidates,
                                 List<ProjectionDiagnostic> diagnostics,
                                 Set<String> reportedDiagnosticKeys) {
        String datatype = element.datatype() == null ? null : element.datatype().replace('-', '_');
        if (datatype == null || "ANY".equals(datatype)) {
            recordDiagnostic("value", element.path(), "insufficient_source_datatype_precision",
                    "No safe FHIR value[x] type mapping found for datatype '" + element.datatype() + "'.",
                    inference != null ? inference.confidence() : BranchInferenceEngine.BranchConfidence.UNSAFE,
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        List<BranchInferenceEngine.BranchAlternative> alternatives = inference == null
                ? List.of()
                : inference.alternatives().stream()
                .filter(alternative -> "value".equals(alternative.targetPath()))
                .filter(alternative -> datatype.equals(alternative.sourceType()))
                .filter(alternative -> alternative.createdType() != null)
                .toList();
        List<String> fhirTypes = alternatives.stream()
                .map(BranchInferenceEngine.BranchAlternative::createdType)
                .filter(Objects::nonNull)
                .map(this::normalizeFhirType)
                .distinct()
                .sorted()
                .toList();
        if (fhirTypes.isEmpty()) {
            String mappedType = fallbackValueType(datatype);
            if (mappedType != null) {
                fhirTypes = List.of(mappedType);
            }
        }
        if (fhirTypes.isEmpty()) {
            recordDiagnostic("value", element.path(), "insufficient_source_datatype_precision",
                    "No safe FHIR value[x] type mapping found for datatype '" + element.datatype() + "'.",
                    inference != null ? inference.confidence() : BranchInferenceEngine.BranchConfidence.UNSAFE,
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        if (element.cardinality() != null) {
            candidates.add(ProjectionCandidate.cardinality("value[x]", element.cardinality()));
        }
        for (String type : fhirTypes) {
            candidates.add(ProjectionCandidate.onlyType("value[x]", type));
        }
        String typedValuePath = typedValuePath(fhirTypes);
        for (IRBinding binding : element.bindings()) {
            if (typedValuePath != null && binding.valueSetRef() != null && !binding.valueSetRef().isBlank()) {
                candidates.add(ProjectionCandidate.binding(typedValuePath, binding.valueSetRef(), binding.strength()));
            }
        }
        if (element.fixedValue() != null) {
            String targetPath = typedValuePath == null ? "value[x]" : typedValuePath;
            IRFixedValueType type = element.fixedValueType() == null ? defaultFixedTypeForPath(targetPath) : element.fixedValueType();
            candidates.add(ProjectionCandidate.fixed(targetPath, element.fixedValue(), type));
        }
        if (element.shortDescription() != null && !element.shortDescription().isBlank()) {
            candidates.add(ProjectionCandidate.shortDescription("value[x]", element.shortDescription()));
        }
    }

    private void emitCodeableConceptBranch(IRElementConstraint element,
                                           BranchInferenceEngine.BranchInference inference,
                                           String targetRoot,
                                           Set<ProjectionCandidate> candidates,
                                           List<ProjectionDiagnostic> diagnostics,
                                           Set<String> reportedDiagnosticKeys) {
        String sourceRoot = rootSegment(element.path());
        if (inference == null || !inference.targetsObservationRoot(targetRoot)) {
            recordDiagnostic(sourceRoot, element.path(), "unsupported_branch",
                    "No safe FHIR path mapping found for CDA branch '" + sourceRoot + "'.",
                    inference != null ? inference.confidence() : BranchInferenceEngine.BranchConfidence.UNSAFE,
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        String suffix = element.path().equals(sourceRoot) ? "" : element.path().substring(sourceRoot.length());
        String targetPath = switch (suffix) {
            case "" -> targetRoot;
            case ".code" -> targetRoot + ".coding.code";
            case ".codeSystem" -> targetRoot + ".coding.system";
            case ".displayName" -> targetRoot + ".coding.display";
            default -> null;
        };
        if (targetPath == null) {
            recordDiagnostic(sourceRoot, element.path(), "branch_partially_supported",
                    "CDA branch '" + sourceRoot + "' maps to FHIR Observation." + targetRoot + ", but path '" + element.path() + "' is not yet safely projected.",
                    inference.confidence(),
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        emitGenericRules(element, targetPath, candidates);
    }

    private void emitReferenceBranch(IRElementConstraint element,
                                     BranchInferenceEngine.BranchInference inference,
                                     String targetRoot,
                                     Set<ProjectionCandidate> candidates,
                                     List<ProjectionDiagnostic> diagnostics,
                                     Set<String> reportedDiagnosticKeys) {
        String sourceRoot = rootSegment(element.path());
        if (inference == null || !inference.targetsObservationRoot(targetRoot)) {
            recordDiagnostic(sourceRoot, element.path(), "unsupported_branch",
                    "No safe FHIR path mapping found for CDA branch '" + sourceRoot + "'.",
                    inference != null ? inference.confidence() : BranchInferenceEngine.BranchConfidence.UNSAFE,
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        if (!element.path().equals(sourceRoot)) {
            recordDiagnostic(sourceRoot, element.path(), "runtime_only_reference_creation",
                    "CDA branch '" + sourceRoot + "' participates in runtime reference construction and path '" + element.path() + "' is not emitted as a standalone profile rule.",
                    inference.confidence(),
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        emitGenericRules(element, targetRoot, candidates);
    }

    private void emitPerformerBranch(IRElementConstraint element,
                                     BranchInferenceEngine.BranchInference directInference,
                                     BranchInferenceEngine.BranchInference participantInference,
                                     Set<ProjectionCandidate> candidates,
                                     List<ProjectionDiagnostic> diagnostics,
                                     Set<String> reportedDiagnosticKeys) {
        BranchInferenceEngine.BranchInference effectiveInference = directInference;
        if (effectiveInference == null || !effectiveInference.targetsObservationRoot("performer")) {
            if (participantInference != null && participantInference.targetsObservationRoot("performer")) {
                effectiveInference = participantInference;
            }
        }
        if (effectiveInference == null || !effectiveInference.targetsObservationRoot("performer")) {
            recordDiagnostic("performer", element.path(), "unsupported_branch",
                    "No safe FHIR path mapping found for CDA branch 'performer'.",
                    effectiveInference != null ? effectiveInference.confidence() : BranchInferenceEngine.BranchConfidence.UNSAFE,
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        if (!"performer".equals(element.path())) {
            return;
        }
        emitGenericRules(element, "performer", candidates);
    }

    private void emitEntryRelationshipBranch(IRElementConstraint element,
                                             BranchInferenceEngine.BranchInference inference,
                                             SemanticMappingModel model,
                                             EntryRelationshipFlavor flavor,
                                             Set<ProjectionCandidate> candidates,
                                             List<ProjectionDiagnostic> diagnostics,
                                             Set<String> reportedDiagnosticKeys) {
        if (isIgnoredEntryRelationshipPath(element.path())) {
            return;
        }
        String targetRoot = entryRelationshipTargetRoot(element.path(), model, flavor);
        if (targetRoot == null) {
            recordDiagnostic("entryRelationship", element.path(), "mapping_policy_required",
                    "CDA branch 'entryRelationship' mixes multiple relationship flavors. Choose a specific child mapping such as act -> note or observation -> hasMember.",
                    inference != null ? inference.confidence() : BranchInferenceEngine.BranchConfidence.UNSAFE,
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        if (inference == null || !inference.targetsObservationRoot(targetRoot)) {
            recordDiagnostic("entryRelationship", element.path(), "unsupported_branch",
                    "No safe FHIR path mapping found for CDA branch 'entryRelationship'.",
                    inference != null ? inference.confidence() : BranchInferenceEngine.BranchConfidence.UNSAFE,
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        if ("entryRelationship".equals(element.path()) || "entryRelationship.act".equals(element.path()) || "entryRelationship.observation".equals(element.path())) {
            emitGenericRules(element, targetRoot, candidates);
            return;
        }
        if ("note".equals(targetRoot) && "entryRelationship.act.text".equals(element.path())) {
            emitGenericRules(element, "note.text", candidates);
            return;
        }
        if ("hasMember".equals(targetRoot) && element.path().startsWith("entryRelationship.observation.")) {
            return;
        }
        if ("note".equals(targetRoot) && element.path().startsWith("entryRelationship.act.")) {
            return;
        }
        recordDiagnostic("entryRelationship", element.path(), "runtime_only_reference_creation",
                "CDA branch 'entryRelationship' maps to FHIR Observation." + targetRoot + ", but path '" + element.path() + "' is handled during runtime relationship construction.",
                inference.confidence(),
                diagnostics,
                reportedDiagnosticKeys);
    }

    private String entryRelationshipTargetRoot(String sourcePath, SemanticMappingModel model, EntryRelationshipFlavor flavor) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return null;
        }
        if ("entryRelationship.typeCode".equals(sourcePath) || "entryRelationship.inversionInd".equals(sourcePath)) {
            return null;
        }
        if (sourcePath.startsWith("entryRelationship.act")) {
            return hasTargetForSourcePath(model, "entryRelationship.act", "note") ? "note" : null;
        }
        if (sourcePath.startsWith("entryRelationship.observation")) {
            return hasTargetForSourcePath(model, "entryRelationship.observation", "hasMember") ? "hasMember" : null;
        }
        if ("entryRelationship".equals(sourcePath)) {
            return switch (flavor) {
                case NOTE -> "note";
                case HAS_MEMBER -> "hasMember";
                default -> null;
            };
        }
        return null;
    }

    private boolean hasTargetForSourcePath(SemanticMappingModel model,
                                           String sourcePath,
                                           String targetRoot) {
        for (SemanticRule rule : model.allRules()) {
            if (!matchesSourcePath(rule, sourcePath)) {
                continue;
            }
            if (matchesTargetRoots(rule, Set.of(targetRoot))) {
                return true;
            }
        }
        return false;
    }

    private EntryRelationshipFlavor deriveEntryRelationshipFlavor(IRTemplate template, SemanticMappingModel model) {
        boolean hasAct = false;
        boolean hasObservation = false;
        for (IRElementConstraint element : template.elements()) {
            String path = element.path();
            if (path == null || path.isBlank()) {
                continue;
            }
            if (path.startsWith("entryRelationship.act")) {
                hasAct = true;
            } else if (path.startsWith("entryRelationship.observation")) {
                hasObservation = true;
            }
        }
        boolean supportsNote = hasTargetForSourcePath(model, "entryRelationship.act", "note");
        boolean supportsHasMember = hasTargetForSourcePath(model, "entryRelationship.observation", "hasMember");
        if (hasAct && !hasObservation && supportsNote) {
            return EntryRelationshipFlavor.NOTE;
        }
        if (hasObservation && !hasAct && supportsHasMember) {
            return EntryRelationshipFlavor.HAS_MEMBER;
        }
        if (hasAct && hasObservation && supportsNote && supportsHasMember) {
            return EntryRelationshipFlavor.AMBIGUOUS;
        }
        return EntryRelationshipFlavor.NONE;
    }

    private boolean isIgnoredEntryRelationshipPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        if (IGNORED_ENTRY_RELATIONSHIP_EXACT_PATHS.contains(path)) {
            return true;
        }
        for (String prefix : IGNORED_ENTRY_RELATIONSHIP_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void emitReferenceLinkBranch(IRElementConstraint element,
                                         BranchInferenceEngine.BranchInference inference,
                                         Set<ProjectionCandidate> candidates,
                                         List<ProjectionDiagnostic> diagnostics,
                                         Set<String> reportedDiagnosticKeys) {
        if (inference != null && inference.targetsObservationRoot("derivedFrom")) {
            if ("reference".equals(element.path())) {
                emitGenericRules(element, "derivedFrom", candidates);
            } else {
                recordDiagnostic("reference", element.path(), "runtime_only_reference_creation",
                        "CDA branch 'reference' maps to FHIR Observation.derivedFrom, but child path '" + element.path() + "' is handled during runtime Reference construction.",
                        inference.confidence(),
                        diagnostics,
                        reportedDiagnosticKeys);
            }
            return;
        }
        recordDiagnostic("reference", element.path(), "mapping_policy_required",
                "CDA branch 'reference' needs an explicit relationship policy before it can be projected. Choose Observation.derivedFrom, hasMember, partOf, note, or an extension based on the StructureMap intent.",
                inference != null ? inference.confidence() : BranchInferenceEngine.BranchConfidence.UNSAFE,
                diagnostics,
                reportedDiagnosticKeys);
    }

    private void emitMethodBranch(IRElementConstraint element,
                                  BranchInferenceEngine.BranchInference inference,
                                  Set<ProjectionCandidate> candidates,
                                  List<ProjectionDiagnostic> diagnostics,
                                  Set<String> reportedDiagnosticKeys) {
        if (inference == null || !inference.targetsObservationRoot("method")) {
            recordDiagnostic("methodCode", element.path(), "no_target_branch_in_map",
                    "The supplied StructureMap does not expose a safe Observation.method branch for CDA branch 'methodCode'.",
                    inference != null ? inference.confidence() : BranchInferenceEngine.BranchConfidence.UNSAFE,
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        emitCodeableConceptBranch(element, inference, "method", candidates, diagnostics, reportedDiagnosticKeys);
    }

    private boolean isAuthorAbsorbedByNote(BranchInferenceEngine.BranchInference entryRelationshipInference) {
        return entryRelationshipInference != null && entryRelationshipInference.targetsObservationRoot("note");
    }

    private void emitReferenceRangeBranch(IRElementConstraint element,
                                          BranchInferenceEngine.BranchInference inference,
                                          Set<ProjectionCandidate> candidates,
                                          List<ProjectionDiagnostic> diagnostics,
                                          Set<String> reportedDiagnosticKeys) {
        if (inference == null || !inference.targetsObservationRoot("referenceRange")) {
            recordDiagnostic("referenceRange", element.path(), "unsupported_branch",
                    "No safe FHIR path mapping found for CDA branch 'referenceRange'.",
                    inference != null ? inference.confidence() : BranchInferenceEngine.BranchConfidence.UNSAFE,
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        String targetPath = switch (element.path()) {
            case "referenceRange" -> "referenceRange";
            case "referenceRange.observationRange.value.low" -> "referenceRange.low";
            case "referenceRange.observationRange.value.high" -> "referenceRange.high";
            case "referenceRange.observationRange.interpretationCode" -> "referenceRange.type";
            default -> null;
        };
        if (targetPath == null) {
            recordDiagnostic("referenceRange", element.path(), "branch_partially_supported",
                    "CDA branch 'referenceRange' maps to FHIR Observation.referenceRange, but path '" + element.path() + "' is not yet safely projected.",
                    inference.confidence(),
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        emitGenericRules(element, targetPath, candidates);
    }

    private boolean emitDirectBranch(IRElementConstraint element,
                                     String targetRoot,
                                     BranchInferenceEngine.BranchInference inference,
                                     Set<ProjectionCandidate> candidates,
                                     List<ProjectionDiagnostic> diagnostics,
                                     Set<String> reportedDiagnosticKeys,
                                     boolean allowChildPaths) {
        String sourceRoot = rootSegment(element.path());
        if (inference == null || !inference.targetsObservationRoot(targetRoot)) {
            return false;
        }
        if (!allowChildPaths && !element.path().equals(sourceRoot)) {
            recordDiagnostic(sourceRoot, element.path(), "branch_partially_supported",
                    "CDA branch '" + sourceRoot + "' maps to FHIR Observation." + targetRoot + ", but path '" + element.path() + "' is not yet safely projected.",
                    inference.confidence(),
                    diagnostics,
                    reportedDiagnosticKeys);
            return true;
        }
        emitGenericRules(element, targetRoot, candidates);
        return true;
    }

    private void emitGenericRules(IRElementConstraint element,
                                  String targetPath,
                                  Set<ProjectionCandidate> candidates) {
        if (element.cardinality() != null) {
            candidates.add(ProjectionCandidate.cardinality(targetPath, element.cardinality()));
        }
        if (element.fixedValue() != null) {
            IRFixedValueType type = element.fixedValueType() == null ? defaultFixedTypeForPath(targetPath) : element.fixedValueType();
            candidates.add(ProjectionCandidate.fixed(targetPath, element.fixedValue(), type));
        }
        for (IRBinding binding : element.bindings()) {
            if (binding.valueSetRef() != null && !binding.valueSetRef().isBlank()) {
                candidates.add(ProjectionCandidate.binding(targetPath, binding.valueSetRef(), binding.strength()));
            }
        }
        if (element.shortDescription() != null && !element.shortDescription().isBlank()) {
            candidates.add(ProjectionCandidate.shortDescription(targetPath, element.shortDescription()));
        }
    }

    private void recordDiagnostic(String branch,
                                  String path,
                                  String reasonCode,
                                  String message,
                                  BranchInferenceEngine.BranchConfidence confidence,
                                  List<ProjectionDiagnostic> diagnostics,
                                  Set<String> reportedDiagnosticKeys) {
        String key = branch + "|" + reasonCode;
        if (!reportedDiagnosticKeys.add(key)) {
            return;
        }
        diagnostics.add(new ProjectionDiagnostic(branch, path, reasonCode, confidence, message));
    }

    private String rootSegment(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        int dot = path.indexOf('.');
        return dot >= 0 ? path.substring(0, dot) : path;
    }

    private String normalizeFhirType(String type) {
        if (type == null || type.isBlank()) {
            return "string";
        }
        if ("datetime".equalsIgnoreCase(type)) {
            return "dateTime";
        }
        return type;
    }

    private String fallbackValueType(String cdaDatatype) {
        return switch (cdaDatatype) {
            case "CD", "CE", "CV", "CO", "CS" -> "CodeableConcept";
            case "PQ", "IVL_PQ", "RTO_PQ_PQ" -> "Quantity";
            case "ST", "ED", "SC" -> "string";
            case "TS", "IVL_TS", "SXCM_TS", "SXPR_TS", "PIVL_TS", "EIVL_TS" -> "dateTime";
            case "BL" -> "boolean";
            case "INT", "INT_POS" -> "integer";
            case "REAL" -> "decimal";
            default -> null;
        };
    }

    private String typedValuePath(List<String> fhirTypes) {
        if (fhirTypes.size() != 1) {
            return null;
        }
        return switch (fhirTypes.get(0)) {
            case "CodeableConcept" -> "valueCodeableConcept";
            case "Quantity" -> "valueQuantity";
            case "string" -> "valueString";
            case "dateTime" -> "valueDateTime";
            case "boolean" -> "valueBoolean";
            case "integer" -> "valueInteger";
            case "decimal" -> "valueDecimal";
            default -> null;
        };
    }

    private IRFixedValueType defaultFixedTypeForPath(String path) {
        if (path == null) {
            return IRFixedValueType.STRING;
        }
        if (path.endsWith(".code") || "status".equals(path)) {
            return IRFixedValueType.CODE;
        }
        return IRFixedValueType.STRING;
    }

    record ObservationProjectionResult(String parent,
                                       List<ProjectionCandidate> candidates,
                                       List<ProjectionDiagnostic> diagnostics,
                                       Set<SemanticRule> usedRules) {
    }

    record ProjectionDiagnostic(String sourceBranch,
                                String sourcePath,
                                String reasonCode,
                                BranchInferenceEngine.BranchConfidence confidence,
                                String message) {
    }

    record ProjectionCandidate(String targetPath,
                               ProjectionKind kind,
                               String value,
                               IRFixedValueType fixedValueType,
                               IRBindingStrength bindingStrength) {
        static ProjectionCandidate cardinality(String targetPath, IRCardinality cardinality) {
            return new ProjectionCandidate(targetPath, ProjectionKind.CARDINALITY, cardinality.format(), null, null);
        }

        static ProjectionCandidate fixed(String targetPath, String value, IRFixedValueType type) {
            return new ProjectionCandidate(targetPath, ProjectionKind.FIXED, value, type, null);
        }

        static ProjectionCandidate binding(String targetPath, String valueSetRef, IRBindingStrength strength) {
            return new ProjectionCandidate(targetPath, ProjectionKind.BINDING, valueSetRef, null, strength);
        }

        static ProjectionCandidate shortDescription(String targetPath, String shortDescription) {
            return new ProjectionCandidate(targetPath, ProjectionKind.SHORT, shortDescription, null, null);
        }

        static ProjectionCandidate onlyType(String targetPath, String type) {
            return new ProjectionCandidate(targetPath, ProjectionKind.ONLY_TYPE, type, null, null);
        }

        String toFshLine() {
            return switch (kind) {
                case CARDINALITY -> "* " + targetPath + " " + value;
                case FIXED -> "* " + targetPath + " = " + formatFixedValue(value, fixedValueType);
                case BINDING -> "* " + targetPath + " from " + value + " (" + bindingStrength.name().toLowerCase(Locale.ROOT) + ")";
                case SHORT -> "* " + targetPath + " ^short = \"" + FshUtil.escape(value) + "\"";
                case ONLY_TYPE -> "* " + targetPath + " only " + value;
            };
        }

        private String formatFixedValue(String fixedValue, IRFixedValueType type) {
            if (type == null || type == IRFixedValueType.STRING) {
                return "\"" + FshUtil.escape(fixedValue) + "\"";
            }
            if (type == IRFixedValueType.CODE) {
                return "#" + fixedValue;
            }
            if (type == IRFixedValueType.BOOLEAN) {
                if ("1".equals(fixedValue)) {
                    return "true";
                }
                if ("0".equals(fixedValue)) {
                    return "false";
                }
                return fixedValue.toLowerCase(Locale.ROOT);
            }
            return "\"" + FshUtil.escape(fixedValue) + "\"";
        }
    }

    enum ProjectionKind {
        CARDINALITY,
        FIXED,
        BINDING,
        SHORT,
        ONLY_TYPE
    }
}
