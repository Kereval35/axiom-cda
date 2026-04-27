package net.ihe.gazelle.axiomcda.engine.business;

import java.util.*;

class BranchInferenceEngine {

    List<BranchInference> infer(StructureMapSemanticAnalyzer.StructureMapSemanticModel model) {
        Map<String, BranchAccumulator> accumulators = new LinkedHashMap<>();
        for (StructureMapSemanticAnalyzer.SemanticRule rule : model.allRules()) {
            collectRule(rule, accumulators, rootSegment(rule.primarySourcePath()));
        }
        List<BranchInference> result = new ArrayList<>();
        for (BranchAccumulator accumulator : accumulators.values()) {
            result.add(accumulator.toInference());
        }
        result.sort(Comparator.comparing(BranchInference::sourceBranch, Comparator.nullsLast(String::compareTo)));
        return result;
    }

    private void collectRule(StructureMapSemanticAnalyzer.SemanticRule rule,
                             Map<String, BranchAccumulator> accumulators,
                             String inheritedBranch) {
        String branch = firstNonBlank(
                rule.branchLineage().isEmpty() ? null : rule.branchLineage().get(rule.branchLineage().size() - 1),
                rootSegment(rule.primarySourcePath()),
                inheritedBranch
        );
        if (branch == null || branch.isBlank()) {
            return;
        }
        BranchAccumulator accumulator = accumulators.computeIfAbsent(branch, BranchAccumulator::new);
        accumulator.record(rule);
        for (StructureMapSemanticAnalyzer.SemanticRule child : rule.children()) {
            collectRule(child, accumulators, branch);
        }
    }

    private String rootSegment(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        int dot = path.indexOf('.');
        return dot >= 0 ? path.substring(0, dot) : path;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static final class BranchAccumulator {
        private final String sourceBranch;
        private final Set<String> sourcePaths = new LinkedHashSet<>();
        private final Set<String> observationTargetPaths = new LinkedHashSet<>();
        private final Set<String> externalTargetPaths = new LinkedHashSet<>();
        private final Set<String> dependentGroups = new LinkedHashSet<>();
        private final Set<String> conditions = new LinkedHashSet<>();
        private final Map<String, String> constantsByTargetPath = new LinkedHashMap<>();
        private final List<BranchAlternative> alternatives = new ArrayList<>();
        private boolean anyConditional;
        private boolean runtimeOnlyMechanics;

        private BranchAccumulator(String sourceBranch) {
            this.sourceBranch = sourceBranch;
        }

        void record(StructureMapSemanticAnalyzer.SemanticRule rule) {
            if (rule.conditional()) {
                anyConditional = true;
            }
            for (StructureMapSemanticAnalyzer.SourceNode source : rule.sources()) {
                if (source.path() != null && !source.path().isBlank()) {
                    sourcePaths.add(source.path());
                }
                if (source.condition() != null && !source.condition().isBlank()) {
                    conditions.add(source.condition());
                }
            }
            for (StructureMapSemanticAnalyzer.TargetNode target : rule.targets()) {
                if (target.transform() != null && ("uuid".equals(target.transform()) || "append".equals(target.transform()))) {
                    runtimeOnlyMechanics = true;
                }
                if (target.path() == null || target.path().isBlank()) {
                    continue;
                }
                if (target.path().startsWith("@")) {
                    externalTargetPaths.add(target.path());
                } else {
                    observationTargetPaths.add(target.path());
                    if (target.constantValue() != null) {
                        constantsByTargetPath.put(target.path(), target.constantValue());
                    }
                    alternatives.add(new BranchAlternative(
                            target.path(),
                            primarySourceType(rule.sources()),
                            target.createdType(),
                            target.conditional(),
                            target.transform(),
                            rule.groupName(),
                            List.copyOf(rule.dependentCalls().stream().map(StructureMapSemanticAnalyzer.DependentCallNode::name).toList())
                    ));
                }
            }
            for (StructureMapSemanticAnalyzer.DependentCallNode dependentCall : rule.dependentCalls()) {
                dependentGroups.add(dependentCall.name());
            }
        }

        BranchInference toInference() {
            Set<String> targetRoots = new LinkedHashSet<>();
            for (String path : observationTargetPaths) {
                int dot = path.indexOf('.');
                targetRoots.add(dot >= 0 ? path.substring(0, dot) : path);
            }
            BranchConfidence confidence;
            if (observationTargetPaths.isEmpty() && (!externalTargetPaths.isEmpty() || runtimeOnlyMechanics)) {
                confidence = BranchConfidence.RUNTIME_ONLY;
            } else if (targetRoots.size() == 1 && !anyConditional) {
                confidence = BranchConfidence.SAFE;
            } else if (targetRoots.size() == 1) {
                confidence = BranchConfidence.LIKELY;
            } else {
                confidence = BranchConfidence.UNSAFE;
            }
            return new BranchInference(
                    sourceBranch,
                    List.copyOf(sourcePaths),
                    List.copyOf(observationTargetPaths),
                    List.copyOf(externalTargetPaths),
                    List.copyOf(dependentGroups),
                    List.copyOf(conditions),
                    Map.copyOf(constantsByTargetPath),
                    List.copyOf(alternatives),
                    anyConditional,
                    runtimeOnlyMechanics,
                    confidence
            );
        }

        private String primarySourceType(List<StructureMapSemanticAnalyzer.SourceNode> sources) {
            for (StructureMapSemanticAnalyzer.SourceNode source : sources) {
                if (source.type() != null && !source.type().isBlank()) {
                    return source.type();
                }
            }
            return null;
        }
    }

    enum BranchConfidence {
        SAFE,
        LIKELY,
        UNSAFE,
        RUNTIME_ONLY
    }

    record BranchAlternative(String targetPath,
                             String sourceType,
                             String createdType,
                             boolean conditional,
                             String transform,
                             String groupName,
                             List<String> dependentGroups) {
    }

    record BranchInference(String sourceBranch,
                           List<String> sourcePaths,
                           List<String> observationTargetPaths,
                           List<String> externalTargetPaths,
                           List<String> dependentGroups,
                           List<String> conditions,
                           Map<String, String> constantsByTargetPath,
                           List<BranchAlternative> alternatives,
                           boolean conditional,
                           boolean runtimeOnlyMechanics,
                           BranchConfidence confidence) {
        boolean targetsObservationRoot(String root) {
            return observationTargetPaths.stream().anyMatch(path -> path.equals(root) || path.startsWith(root + "."));
        }
    }
}
