package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.ir.*;
import net.ihe.gazelle.axiomcda.engine.util.FshUtil;

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

    ObservationProjectionResult interpret(IRTemplate template,
                                          StructureMapSemanticAnalyzer.StructureMapSemanticModel model,
                                          List<BranchInferenceEngine.BranchInference> inferences) {
        String parent = resolveParent(model);
        LinkedHashSet<ProjectionCandidate> candidates = new LinkedHashSet<>();
        List<ProjectionDiagnostic> diagnostics = new ArrayList<>();
        Map<String, BranchInferenceEngine.BranchInference> inferenceByBranch = new LinkedHashMap<>();
        for (BranchInferenceEngine.BranchInference inference : inferences) {
            inferenceByBranch.put(inference.sourceBranch(), inference);
        }

        seedGlobalConstants(model, candidates);

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
                    if (!emitDirectBranch(element, "identifier", inference, candidates, diagnostics, reportedDiagnosticKeys, false)) {
                        recordDiagnostic(root, path, "unsupported_branch", "No safe FHIR path mapping found for CDA branch '" + root + "'.", BranchInferenceEngine.BranchConfidence.UNSAFE, diagnostics, reportedDiagnosticKeys);
                    }
                }
                case "code" -> emitCodeBranch(element, inference, candidates, diagnostics, reportedDiagnosticKeys);
                case "statusCode" -> emitStatusBranch(element, inference, candidates, diagnostics, reportedDiagnosticKeys);
                case "effectiveTime" -> emitEffectiveBranch(element, inference, candidates, diagnostics, reportedDiagnosticKeys);
                case "text" -> emitTextBranch(element, candidates);
                case "value" -> emitValueBranch(element, inference, candidates, diagnostics, reportedDiagnosticKeys);
                case "interpretationCode" -> emitCodeableConceptBranch(element, inference, "interpretation", candidates, diagnostics, reportedDiagnosticKeys);
                case "methodCode" -> emitMethodBranch(element, inference, candidates, diagnostics, reportedDiagnosticKeys);
                case "participant" -> emitReferenceBranch(element, inference, "performer", candidates, diagnostics, reportedDiagnosticKeys);
                case "performer" -> emitPerformerBranch(element, inference, inferenceByBranch.get("participant"), candidates, diagnostics, reportedDiagnosticKeys);
                case "entryRelationship" -> emitEntryRelationshipBranch(element, inference, candidates, diagnostics, reportedDiagnosticKeys);
                case "reference" -> emitReferenceLinkBranch(element, inference, candidates, diagnostics, reportedDiagnosticKeys);
                case "referenceRange" -> emitReferenceRangeBranch(element, inference, candidates, diagnostics, reportedDiagnosticKeys);
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
        return new ObservationProjectionResult(parent, List.copyOf(candidates), List.copyOf(diagnostics));
    }

    private void seedGlobalConstants(StructureMapSemanticAnalyzer.StructureMapSemanticModel model,
                                     Set<ProjectionCandidate> candidates) {
        for (StructureMapSemanticAnalyzer.SemanticRule rule : model.allRules()) {
            for (StructureMapSemanticAnalyzer.TargetNode target : rule.targets()) {
                if (target.path() == null || target.path().isBlank() || target.conditional()) {
                    continue;
                }
                if ("category.coding.system".equals(target.path()) || "category.coding.code".equals(target.path())) {
                    if (target.constantValue() != null) {
                        candidates.add(ProjectionCandidate.fixed(target.path(), target.constantValue(), defaultFixedTypeForPath(target.path())));
                    }
                }
            }
        }
    }

    private String resolveParent(StructureMapSemanticAnalyzer.StructureMapSemanticModel model) {
        for (StructureMapSemanticAnalyzer.SemanticRule rule : model.allRules()) {
            for (StructureMapSemanticAnalyzer.TargetNode target : rule.targets()) {
                if ("meta.profile".equals(target.path()) && target.constantValue() != null) {
                    return target.constantValue();
                }
            }
        }
        return DEFAULT_PARENT;
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
                                             Set<ProjectionCandidate> candidates,
                                             List<ProjectionDiagnostic> diagnostics,
                                             Set<String> reportedDiagnosticKeys) {
        if (inference == null || !inference.targetsObservationRoot("note")) {
            recordDiagnostic("entryRelationship", element.path(), "unsupported_branch",
                    "No safe FHIR path mapping found for CDA branch 'entryRelationship'.",
                    inference != null ? inference.confidence() : BranchInferenceEngine.BranchConfidence.UNSAFE,
                    diagnostics,
                    reportedDiagnosticKeys);
            return;
        }
        if ("entryRelationship".equals(element.path())) {
            emitGenericRules(element, "note", candidates);
            return;
        }
        if ("entryRelationship.typeCode".equals(element.path())) {
            return;
        }
        if ("entryRelationship.inversionInd".equals(element.path())) {
            return;
        }
        recordDiagnostic("entryRelationship", element.path(), "runtime_only_reference_creation",
                "CDA branch 'entryRelationship' participates in runtime reference construction and path '" + element.path() + "' is not emitted as a standalone profile rule.",
                inference.confidence(),
                diagnostics,
                reportedDiagnosticKeys);
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
                                       List<ProjectionDiagnostic> diagnostics) {
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
