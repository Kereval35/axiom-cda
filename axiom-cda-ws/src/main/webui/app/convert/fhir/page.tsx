"use client";

import React from "react";
import Link from "next/link";
import { compileFhirSushiAction, convertFhirAction, getFhirBuiltInMappingPresetsAction, getFhirPackagePresetsAction } from "../../../actions/generation";
import { FshProfileModal } from "../../../components/FshProfileModal";
import { useLanguage } from "../../../components/LanguageProvider";
import { FshHighlighter } from "../../../components/FshHighlighter";
import { JsonHighlighter } from "../../../components/JsonHighlighter";
import { FhirBuiltInMappingPreset, FhirPackagePreset, FshProfile, IRTemplate, SushiCompileResult } from "../../../types/generation";
import { loadFhirConversionSession } from "../../../utils/fhirConversionSession";
import { formatFsh } from "../../../utils/fshFormatter";

export default function FhirConversionPage() {
    const { t } = useLanguage();
    const [selectedTemplate, setSelectedTemplate] = React.useState<IRTemplate | null>(null);
    const [selectedProfile, setSelectedProfile] = React.useState<FshProfile | null>(null);
    const [useGenericMapping, setUseGenericMapping] = React.useState(true);
    const [structureMap, setStructureMap] = React.useState("");
    const [uploadedFileName, setUploadedFileName] = React.useState<string | null>(null);
    const [result, setResult] = React.useState<FshProfile | null>(null);
    const [mappingRulesName, setMappingRulesName] = React.useState<string | null>(null);
    const [mappingRulesFsh, setMappingRulesFsh] = React.useState<string | null>(null);
    const [usedMappingRulesName, setUsedMappingRulesName] = React.useState<string | null>(null);
    const [usedMappingRulesFsh, setUsedMappingRulesFsh] = React.useState<string | null>(null);
    const [showMappingRulesModal, setShowMappingRulesModal] = React.useState(false);
    const [showUsedMappingRulesModal, setShowUsedMappingRulesModal] = React.useState(false);
    const [diagnostics, setDiagnostics] = React.useState<string[]>([]);
    const [loading, setLoading] = React.useState(false);
    const [error, setError] = React.useState<string | null>(null);
    const [viewerMode, setViewerMode] = React.useState<"cda-fsh" | "ir" | null>(null);
    const [resultFormatted, setResultFormatted] = React.useState(false);
    const [isFormattingResult, setIsFormattingResult] = React.useState(false);
    const [resultCopied, setResultCopied] = React.useState(false);
    const [dependencyPackageId, setDependencyPackageId] = React.useState("");
    const [dependencyVersion, setDependencyVersion] = React.useState("");
    const [builtInMappingPresets, setBuiltInMappingPresets] = React.useState<FhirBuiltInMappingPreset[]>([]);
    const [selectedBuiltInMappingId, setSelectedBuiltInMappingId] = React.useState("");
    const [packagePresets, setPackagePresets] = React.useState<FhirPackagePreset[]>([]);
    const [packagePresetsError, setPackagePresetsError] = React.useState<string | null>(null);
    const [builtInMappingPresetsError, setBuiltInMappingPresetsError] = React.useState<string | null>(null);
    const [selectedPackagePresetKey, setSelectedPackagePresetKey] = React.useState("");
    const [sushiLoading, setSushiLoading] = React.useState(false);
    const [sushiResult, setSushiResult] = React.useState<SushiCompileResult | null>(null);
    const [sushiError, setSushiError] = React.useState<string | null>(null);
    const [sushiLogsOpen, setSushiLogsOpen] = React.useState(false);
    const [structureDefinitionCopied, setStructureDefinitionCopied] = React.useState(false);
    const isObservationTemplate = selectedTemplate?.rootCdaType === "Observation";

    React.useEffect(() => {
        const session = loadFhirConversionSession();
        if (!session) {
            return;
        }
        setSelectedTemplate(session.irTemplates.find((template) => template.id === session.selectedTemplateId) ?? null);
        setSelectedProfile(session.profiles.find((profile) => profile.name === session.selectedProfileName) ?? null);
    }, []);

    React.useEffect(() => {
        if (selectedTemplate && selectedTemplate.rootCdaType !== "Observation") {
            setUseGenericMapping(false);
        }
    }, [selectedTemplate]);

    React.useEffect(() => {
        getFhirBuiltInMappingPresetsAction()
            .then((presets) => {
                setBuiltInMappingPresets(presets);
                setBuiltInMappingPresetsError(null);
                const defaultPreset = presets.find((preset) => preset.defaultSelected) ?? presets[0];
                setSelectedBuiltInMappingId(defaultPreset?.id ?? "");
            })
            .catch((err) => {
                setBuiltInMappingPresets([]);
                setBuiltInMappingPresetsError(err instanceof Error ? err.message : "Unable to load built-in mapping presets");
            });
    }, []);

    React.useEffect(() => {
        getFhirPackagePresetsAction()
            .then((presets) => {
                setPackagePresets(presets);
                setPackagePresetsError(null);
            })
            .catch((err) => {
                setPackagePresets([]);
                setPackagePresetsError(err instanceof Error ? err.message : "Unable to load FHIR package presets");
            });
    }, []);

    const handleUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (!file) {
            return;
        }
        setUploadedFileName(file.name);
        const reader = new FileReader();
        reader.onload = (loadEvent) => {
            setStructureMap(loadEvent.target?.result as string);
        };
        reader.readAsText(file);
    };

    const generate = async () => {
        if (!selectedTemplate || !selectedProfile) {
            return;
        }
        setLoading(true);
        setError(null);
        setResult(null);
        setMappingRulesName(null);
        setMappingRulesFsh(null);
        setUsedMappingRulesName(null);
        setUsedMappingRulesFsh(null);
        setShowMappingRulesModal(false);
        setShowUsedMappingRulesModal(false);
        setResultFormatted(false);
        setResultCopied(false);
        setSushiResult(null);
        setSushiError(null);
        setSushiLogsOpen(false);
        setStructureDefinitionCopied(false);
        try {
            const useBuiltInMapping = selectedTemplate.rootCdaType === "Observation" && useGenericMapping;
            const response = await convertFhirAction({
                sourceProfileName: selectedProfile.name,
                template: selectedTemplate,
                structureMap: useBuiltInMapping ? null : structureMap,
                builtInMappingId: useBuiltInMapping ? selectedBuiltInMappingId || null : null,
            });
            setDiagnostics(response.diagnostics);
            setResult(response.profiles[0] ?? null);
            setMappingRulesName(response.mappingRulesName);
            setMappingRulesFsh(response.mappingRulesFsh);
            setUsedMappingRulesName(response.usedMappingRulesName);
            setUsedMappingRulesFsh(response.usedMappingRulesFsh);
        } catch (err) {
            setError(err instanceof Error ? err.message : "FHIR conversion failed");
        } finally {
            setLoading(false);
        }
    };

    const extractParent = (fshContent: string) => {
        const parentLine = fshContent.split(/\r?\n/).find((line) => line.trim().startsWith("Parent:"));
        return parentLine ? parentLine.trim().substring("Parent:".length).trim() : "";
    };

    const isBaseObservationParent = (parent: string) => {
        return parent === "Observation" || parent === "http://hl7.org/fhir/StructureDefinition/Observation";
    };

    const compileWithSushi = async () => {
        if (!result) {
            return;
        }
        const parent = extractParent(result.content);
        if (!isBaseObservationParent(parent) && (!dependencyPackageId.trim() || !dependencyVersion.trim())) {
            setSushiError("Enter the official IG package id and version before compiling this external parent profile.");
            return;
        }
        setSushiLoading(true);
        setSushiError(null);
        setSushiResult(null);
        setSushiLogsOpen(false);
        setStructureDefinitionCopied(false);
        try {
            const response = await compileFhirSushiAction({
                profileName: result.name,
                fshContent: result.content,
                parent,
                dependencyPackageId: dependencyPackageId.trim() || null,
                dependencyVersion: dependencyVersion.trim() || null,
            });
            setSushiResult(response);
        } catch (err) {
            setSushiError(err instanceof Error ? err.message : "SUSHI compilation failed");
        } finally {
            setSushiLoading(false);
        }
    };

    const packagePresetKey = (preset: FhirPackagePreset) => `${preset.packageId}#${preset.version}`;

    const selectPackagePreset = (key: string) => {
        setSelectedPackagePresetKey(key);
        const preset = packagePresets.find((item) => packagePresetKey(item) === key);
        if (!preset) {
            return;
        }
        setDependencyPackageId(preset.packageId);
        setDependencyVersion(preset.version);
    };

    const toggleResultFormat = () => {
        if (isFormattingResult) {
            return;
        }
        setIsFormattingResult(true);
        setTimeout(() => {
            setResultFormatted((formatted) => !formatted);
            setIsFormattingResult(false);
        }, 50);
    };

    const getDisplayedResultContent = () => {
        if (!result) {
            return "";
        }
        return resultFormatted ? formatFsh(result.content) : result.content;
    };

    const copyResultToClipboard = () => {
        const displayContent = getDisplayedResultContent();
        navigator.clipboard.writeText(displayContent);
        setResultCopied(true);
        setTimeout(() => setResultCopied(false), 2000);
    };

    const downloadResultFsh = () => {
        if (!result) {
            return;
        }
        const blob = new Blob([getDisplayedResultContent()], { type: "text/plain" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = `${result.name}.fsh`;
        link.click();
        URL.revokeObjectURL(url);
    };

    const copyStructureDefinition = () => {
        if (!sushiResult?.structureDefinitionJson) {
            return;
        }
        navigator.clipboard.writeText(sushiResult.structureDefinitionJson);
        setStructureDefinitionCopied(true);
        setTimeout(() => setStructureDefinitionCopied(false), 2000);
    };

    const downloadStructureDefinition = () => {
        if (!sushiResult?.structureDefinitionJson) {
            return;
        }
        const blob = new Blob([sushiResult.structureDefinitionJson], { type: "application/fhir+json" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = sushiResult.generatedFileName ?? `StructureDefinition-${result?.name ?? "generated"}.json`;
        link.click();
        URL.revokeObjectURL(url);
    };

    if (!selectedTemplate || !selectedProfile) {
        return (
            <div className="max-w-5xl mx-auto px-6 py-6">
                <div className="glass rounded-2xl p-6 space-y-4">
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-zinc-100">{t.dashboard.fhirConversionTitle}</h1>
                    <p className="text-sm text-zinc-600 dark:text-zinc-300">{t.dashboard.conversionMissingState}</p>
                    <Link href="/" className="inline-flex items-center rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500">
                        {t.dashboard.backToDashboard}
                    </Link>
                </div>
            </div>
        );
    }

    const irDisplayContent = JSON.stringify(selectedTemplate, null, 2);
    const resultParent = result ? extractParent(result.content) : "";
    const needsSushiDependency = result ? !isBaseObservationParent(resultParent) : false;
    const canCompileWithSushi = Boolean(result && (!needsSushiDependency || (dependencyPackageId.trim() && dependencyVersion.trim())));
    const modalProfile = viewerMode === "ir"
        ? {
            ...selectedProfile,
            name: `${selectedTemplate.displayName || selectedTemplate.name || selectedTemplate.id}-ir`,
            content: irDisplayContent,
            fhirTransformEligible: false,
        }
        : selectedProfile;

    return (
        <div className="max-w-5xl mx-auto px-6 py-6 space-y-6">
            <div className="glass rounded-2xl p-6 space-y-4">
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <h1 className="text-2xl font-semibold text-zinc-900 dark:text-zinc-100">{t.dashboard.fhirConversionTitle}</h1>
                        <p className="mt-2 text-sm text-zinc-600 dark:text-zinc-300">{t.dashboard.fhirConversionSubtitle}</p>
                    </div>
                    <Link href="/" className="inline-flex items-center rounded-lg bg-zinc-200 px-4 py-2 text-sm font-medium text-zinc-800 hover:bg-zinc-300 dark:bg-zinc-800 dark:text-zinc-100 dark:hover:bg-zinc-700">
                        {t.dashboard.backToDashboard}
                    </Link>
                </div>

                <div className="rounded-xl border border-zinc-300 bg-zinc-50/60 p-4 dark:border-zinc-700 dark:bg-zinc-900/40">
                    <div className="text-xs font-medium uppercase tracking-wide text-zinc-500 dark:text-zinc-400">{t.dashboard.selectedTemplate}</div>
                    <div className="mt-2 text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                        {selectedTemplate.displayName || selectedTemplate.name || selectedTemplate.id}
                    </div>
                    <div className="mt-1 flex flex-wrap gap-2 text-xs text-zinc-500 dark:text-zinc-400">
                        <span>{selectedTemplate.id}</span>
                        <span className="rounded bg-indigo-100 px-2 py-0.5 text-indigo-700 dark:bg-indigo-950/40 dark:text-indigo-300">{selectedTemplate.rootCdaType}</span>
                        <span className="rounded bg-cyan-100 px-2 py-0.5 text-cyan-700 dark:bg-cyan-950/40 dark:text-cyan-300">{selectedProfile.name}.fsh</span>
                    </div>
                    <div className="mt-4 flex flex-wrap gap-3">
                        <button
                            onClick={() => setViewerMode("cda-fsh")}
                            className="inline-flex items-center rounded-lg bg-zinc-200 px-4 py-2 text-sm font-medium text-zinc-800 hover:bg-zinc-300 dark:bg-zinc-800 dark:text-zinc-100 dark:hover:bg-zinc-700"
                        >
                            View CDA FSH
                        </button>
                        <button
                            onClick={() => setViewerMode("ir")}
                            className="inline-flex items-center rounded-lg bg-zinc-200 px-4 py-2 text-sm font-medium text-zinc-800 hover:bg-zinc-300 dark:bg-zinc-800 dark:text-zinc-100 dark:hover:bg-zinc-700"
                        >
                            View IR
                        </button>
                    </div>
                </div>

                <div className="space-y-3">
                    <label className="flex items-start gap-3 rounded-xl border border-zinc-300 bg-zinc-50/60 p-4 dark:border-zinc-700 dark:bg-zinc-900/40">
                        <input
                            type="checkbox"
                            checked={isObservationTemplate && useGenericMapping}
                            disabled={!isObservationTemplate}
                            onChange={(event) => setUseGenericMapping(event.target.checked)}
                            className="mt-0.5 h-5 w-5 rounded border-zinc-300 bg-zinc-50 text-indigo-600 focus:ring-indigo-500/50 focus:ring-offset-0 accent-indigo-600 dark:border-zinc-700 dark:bg-zinc-900"
                        />
                        <div className="min-w-0">
                            <div className="text-sm font-medium text-zinc-900 dark:text-zinc-100">{t.dashboard.useGenericMapping}</div>
                            <div className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
                                {isObservationTemplate
                                    ? t.dashboard.useGenericMappingHint
                                    : "Built-in mapping is available only for Observation. Upload a StructureMap JSON for this CDA element."}
                            </div>
                        </div>
                    </label>

                    {(!isObservationTemplate || !useGenericMapping) && (
                        <div className="space-y-2">
                            <label className="text-sm font-medium text-zinc-900 dark:text-zinc-100">{t.dashboard.structureMapLabel}</label>
                            <label className="flex h-44 cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-zinc-300 bg-zinc-50/50 text-center transition-all hover:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-900/50 dark:hover:bg-zinc-800/50">
                                <input type="file" accept=".json,application/json" className="hidden" onChange={handleUpload} />
                                <div className="text-sm font-medium text-zinc-800 dark:text-zinc-100">
                                    {uploadedFileName ?? t.dashboard.uploadStructureMap}
                                </div>
                                <div className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">{t.dashboard.acceptsJson}</div>
                            </label>
                        </div>
                    )}

                    {isObservationTemplate && useGenericMapping && (
                        <div className="space-y-2">
                            <label className="text-sm font-medium text-zinc-900 dark:text-zinc-100">{t.dashboard.builtInMappingLabel}</label>
                            <select
                                value={selectedBuiltInMappingId}
                                onChange={(event) => setSelectedBuiltInMappingId(event.target.value)}
                                className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 outline-none transition-colors focus:border-cyan-500 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-100"
                            >
                                {builtInMappingPresets.map((preset) => (
                                    <option key={preset.id} value={preset.id}>
                                        {preset.label}
                                    </option>
                                ))}
                            </select>
                            {builtInMappingPresetsError ? (
                                <div className="text-xs text-red-600 dark:text-red-400">{builtInMappingPresetsError}</div>
                            ) : (
                                <div className="text-xs text-zinc-500 dark:text-zinc-400">
                                    {builtInMappingPresets.find((preset) => preset.id === selectedBuiltInMappingId)?.description ?? ""}
                                </div>
                            )}
                        </div>
                    )}
                </div>

                <div className="flex justify-end">
                    <button
                        onClick={generate}
                        disabled={loading || (isObservationTemplate && useGenericMapping && !selectedBuiltInMappingId) || ((!isObservationTemplate || !useGenericMapping) && !structureMap)}
                        className="inline-flex items-center rounded-xl bg-gradient-to-r from-cyan-600 to-indigo-600 px-6 py-3 font-semibold text-white transition-all hover:shadow-[0_0_20px_rgba(8,145,178,0.35)] disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {loading ? t.dashboard.generatingFhir : t.dashboard.generateFhir}
                    </button>
                </div>
            </div>

            {error && (
                <div className="rounded-2xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-600 dark:text-red-400">
                    {error}
                </div>
            )}

            {diagnostics.length > 0 && (
                <div className="glass rounded-2xl p-6">
                    <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100">{t.dashboard.conversionDiagnostics}</h2>
                    <ul className="mt-3 space-y-2 text-sm text-zinc-700 dark:text-zinc-300">
                        {diagnostics.map((diagnostic) => (
                            <li key={diagnostic} className="rounded-lg bg-zinc-100/70 px-3 py-2 dark:bg-zinc-900/50">
                                {diagnostic}
                            </li>
                        ))}
                    </ul>
                </div>
            )}

            {result && (
                <div className="glass rounded-2xl p-6">
                    <div className="flex flex-wrap items-start justify-between gap-4">
                        <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100">{t.dashboard.generatedFhirProfiles}</h2>
                        {mappingRulesFsh && mappingRulesName && (
                            <div className="flex flex-wrap gap-2">
                                {usedMappingRulesFsh && usedMappingRulesName && (
                                    <button
                                        onClick={() => setShowUsedMappingRulesModal(true)}
                                        className="inline-flex items-center rounded-lg bg-cyan-600 px-4 py-2 text-sm font-medium text-white hover:bg-cyan-500"
                                    >
                                        {t.dashboard.viewUsedMappingRules}
                                    </button>
                                )}
                                <button
                                    onClick={() => setShowMappingRulesModal(true)}
                                    className="inline-flex items-center rounded-lg bg-zinc-200 px-4 py-2 text-sm font-medium text-zinc-800 hover:bg-zinc-300 dark:bg-zinc-800 dark:text-zinc-100 dark:hover:bg-zinc-700"
                                >
                                    {t.dashboard.viewMappingRules}
                                </button>
                            </div>
                        )}
                    </div>
                    <div className="mt-4 rounded-xl border border-zinc-300 bg-zinc-50/70 p-4 dark:border-zinc-700 dark:bg-zinc-900/50">
                        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                            <div className="font-mono text-sm font-semibold text-zinc-900 dark:text-zinc-100">{result.name}.fsh</div>
                            <div className="flex flex-wrap items-center gap-2">
                            <button
                                onClick={toggleResultFormat}
                                disabled={isFormattingResult}
                                className={`inline-flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                                    resultFormatted
                                        ? "bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300"
                                        : "bg-zinc-100 text-zinc-700 hover:bg-zinc-200 dark:bg-zinc-700 dark:text-zinc-300 dark:hover:bg-zinc-600"
                                }`}
                            >
                                <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 10h16M4 14h16M4 18h16" />
                                </svg>
                                Format
                            </button>
                            <button
                                onClick={copyResultToClipboard}
                                className="inline-flex items-center gap-2 rounded-lg bg-zinc-200 px-3 py-1.5 text-sm font-medium text-zinc-800 transition-colors hover:bg-zinc-300 dark:bg-zinc-700 dark:text-zinc-100 dark:hover:bg-zinc-600"
                            >
                                <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                                </svg>
                                {resultCopied ? "Copied!" : "Copy"}
                            </button>
                            <button
                                onClick={downloadResultFsh}
                                className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-3 py-1.5 text-sm font-medium text-white shadow-lg shadow-indigo-600/20 transition-colors hover:bg-indigo-500"
                            >
                                <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                                </svg>
                                Download
                            </button>
                            </div>
                        </div>
                        {resultFormatted ? (
                            <FshHighlighter code={getDisplayedResultContent()} />
                        ) : (
                            <pre className="overflow-x-auto whitespace-pre rounded-lg bg-zinc-50 p-4 font-mono text-sm text-zinc-800 dark:bg-zinc-950 dark:text-zinc-300">
                                {getDisplayedResultContent()}
                            </pre>
                        )}
                    </div>
                </div>
            )}

            {result && (
                <div className="glass rounded-2xl p-6">
                    <div className="flex flex-wrap items-start justify-between gap-4">
                        <div>
                            <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100">SUSHI Compilation</h2>
                            <p className="mt-1 text-sm text-zinc-600 dark:text-zinc-300">
                                Compile the generated FSH profile into a FHIR StructureDefinition.
                            </p>
                        </div>
                        <button
                            onClick={compileWithSushi}
                            disabled={sushiLoading || !canCompileWithSushi}
                            className="inline-flex items-center rounded-lg bg-cyan-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-cyan-500 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {sushiLoading ? "Compiling..." : "Compile with SUSHI"}
                        </button>
                    </div>

                    <div className="mt-4 rounded-xl border border-zinc-300 bg-zinc-50/70 p-4 dark:border-zinc-700 dark:bg-zinc-900/50">
                        <div className="text-xs font-medium uppercase tracking-wide text-zinc-500 dark:text-zinc-400">Detected parent</div>
                        <div className="mt-2 break-all font-mono text-sm text-zinc-900 dark:text-zinc-100">{resultParent || "Unknown"}</div>
                        {needsSushiDependency ? (
                            <div className="mt-4 space-y-3">
                                <label className="block space-y-1">
                                    <span className="text-xs font-medium text-zinc-600 dark:text-zinc-300">Predefined FHIR package</span>
                                    <select
                                        value={selectedPackagePresetKey}
                                        onChange={(event) => selectPackagePreset(event.target.value)}
                                        className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 outline-none transition-colors focus:border-cyan-500 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-100"
                                    >
                                        <option value="">Manual package</option>
                                        {packagePresets.map((preset) => (
                                            <option key={packagePresetKey(preset)} value={packagePresetKey(preset)}>
                                                {preset.label} ({preset.packageId}#{preset.version})
                                            </option>
                                        ))}
                                    </select>
                                </label>
                                {packagePresetsError && (
                                    <div className="rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-xs text-amber-700 dark:text-amber-300">
                                        {packagePresetsError}
                                    </div>
                                )}
                                {selectedPackagePresetKey && (
                                    <div className="rounded-lg bg-cyan-100 px-3 py-2 text-xs text-cyan-800 dark:bg-cyan-950/40 dark:text-cyan-300">
                                        {packagePresets.find((preset) => packagePresetKey(preset) === selectedPackagePresetKey)?.description
                                            || "Preset package selected. You can still edit the package id or version below."}
                                    </div>
                                )}
                                <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                                    <label className="space-y-1">
                                        <span className="text-xs font-medium text-zinc-600 dark:text-zinc-300">Official IG package id</span>
                                        <input
                                            value={dependencyPackageId}
                                            onChange={(event) => {
                                                setDependencyPackageId(event.target.value);
                                                setSelectedPackagePresetKey("");
                                            }}
                                            placeholder="myhealth.eu.fhir.laboratory"
                                            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 font-mono text-sm text-zinc-900 outline-none transition-colors focus:border-cyan-500 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-100"
                                        />
                                    </label>
                                    <label className="space-y-1">
                                        <span className="text-xs font-medium text-zinc-600 dark:text-zinc-300">Version</span>
                                        <input
                                            value={dependencyVersion}
                                            onChange={(event) => {
                                                setDependencyVersion(event.target.value);
                                                setSelectedPackagePresetKey("");
                                            }}
                                            placeholder="1.0.0"
                                            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 font-mono text-sm text-zinc-900 outline-none transition-colors focus:border-cyan-500 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-100"
                                        />
                                    </label>
                                </div>
                            </div>
                        ) : (
                            <div className="mt-4 rounded-lg bg-emerald-100 px-3 py-2 text-sm text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-300">
                                Base FHIR Observation parent detected. No external IG package is required.
                            </div>
                        )}
                    </div>

                    {sushiError && (
                        <div className="mt-4 rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-600 dark:text-red-400">
                            {sushiError}
                        </div>
                    )}

                    {sushiResult && (
                        <div className={`mt-4 rounded-xl border p-4 ${
                            sushiResult.success
                                ? "border-emerald-500/30 bg-emerald-500/10"
                                : "border-amber-500/30 bg-amber-500/10"
                        }`}>
                            <div className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                                {sushiResult.success ? "SUSHI compilation succeeded" : "SUSHI compilation did not produce a StructureDefinition"}
                            </div>
                            {sushiResult.diagnostics.length > 0 && (
                                <div className="mt-3 flex flex-wrap items-center justify-between gap-3 rounded-lg bg-white/60 px-3 py-2 dark:bg-zinc-950/50">
                                    <div className="text-sm text-zinc-700 dark:text-zinc-300">
                                        {sushiResult.diagnostics.length} SUSHI log {sushiResult.diagnostics.length === 1 ? "entry" : "entries"} available
                                    </div>
                                    <button
                                        onClick={() => setSushiLogsOpen(true)}
                                        className="rounded-lg bg-zinc-200 px-3 py-1.5 text-sm font-medium text-zinc-800 transition-colors hover:bg-zinc-300 dark:bg-zinc-700 dark:text-zinc-100 dark:hover:bg-zinc-600"
                                    >
                                        View SUSHI logs
                                    </button>
                                </div>
                            )}
                            {sushiResult.structureDefinitionJson && (
                                <div className="mt-4 rounded-xl border border-zinc-300 bg-zinc-50/80 p-4 dark:border-zinc-700 dark:bg-zinc-950/60">
                                    <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                                        <div className="font-mono text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                                            {sushiResult.generatedFileName ?? "StructureDefinition.json"}
                                        </div>
                                        <div className="flex gap-2">
                                            <button
                                                onClick={copyStructureDefinition}
                                                className="rounded-lg bg-zinc-200 px-3 py-1.5 text-sm font-medium text-zinc-800 transition-colors hover:bg-zinc-300 dark:bg-zinc-700 dark:text-zinc-100 dark:hover:bg-zinc-600"
                                            >
                                                {structureDefinitionCopied ? "Copied!" : "Copy"}
                                            </button>
                                            <button
                                                onClick={downloadStructureDefinition}
                                                className="rounded-lg bg-indigo-600 px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-indigo-500"
                                            >
                                                Download
                                            </button>
                                        </div>
                                    </div>
                                    <JsonHighlighter json={sushiResult.structureDefinitionJson} />
                                </div>
                            )}
                        </div>
                    )}
                </div>
            )}

            {sushiLogsOpen && sushiResult && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4 py-6">
                    <div className="w-full max-w-4xl rounded-xl border border-zinc-300 bg-white shadow-2xl dark:border-zinc-700 dark:bg-zinc-950">
                        <div className="flex items-center justify-between gap-4 border-b border-zinc-200 px-5 py-4 dark:border-zinc-800">
                            <div>
                                <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100">SUSHI logs</h2>
                                <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
                                    {sushiResult.diagnostics.length} entries from the last compilation
                                </p>
                            </div>
                            <button
                                onClick={() => setSushiLogsOpen(false)}
                                className="rounded-lg bg-zinc-200 px-3 py-1.5 text-sm font-medium text-zinc-800 transition-colors hover:bg-zinc-300 dark:bg-zinc-800 dark:text-zinc-100 dark:hover:bg-zinc-700"
                            >
                                Close
                            </button>
                        </div>
                        <div className="max-h-[70vh] overflow-auto p-5">
                            <ol className="space-y-2">
                                {sushiResult.diagnostics.map((diagnostic, index) => (
                                    <li key={`${diagnostic}-${index}`} className="grid grid-cols-[3rem_1fr] gap-3 rounded-lg bg-zinc-100 px-3 py-2 font-mono text-xs text-zinc-800 dark:bg-zinc-900 dark:text-zinc-200">
                                        <span className="text-right text-zinc-400">{index + 1}</span>
                                        <span className="break-words">{diagnostic}</span>
                                    </li>
                                ))}
                            </ol>
                        </div>
                    </div>
                </div>
            )}

            {viewerMode && (
                <FshProfileModal
                    profile={modalProfile}
                    contentOverride={viewerMode === "ir" ? irDisplayContent : selectedProfile.content}
                    titleOverride={viewerMode === "ir" ? `${selectedTemplate.id}.ir.json` : `${selectedProfile.name}.fsh`}
                    fileNameOverride={viewerMode === "ir" ? `${selectedTemplate.id}.ir.json` : `${selectedProfile.name}.fsh`}
                    onClose={() => setViewerMode(null)}
                />
            )}

            {showMappingRulesModal && mappingRulesFsh && mappingRulesName && (
                <FshProfileModal
                    profile={{
                        name: mappingRulesName,
                        content: mappingRulesFsh,
                        templateId: null,
                        rootCdaType: selectedTemplate.rootCdaType,
                        templateOrigin: "PROJECT",
                        ownershipStatus: "PROJECT",
                        selectionReason: "DIRECT",
                        fhirTransformEligible: false,
                        fhirTransformKind: null,
                        fhirTransformNotice: null,
                    }}
                    contentOverride={mappingRulesFsh}
                    titleOverride={`${mappingRulesName}.fsh`}
                    fileNameOverride={`${mappingRulesName}.fsh`}
                    onClose={() => setShowMappingRulesModal(false)}
                />
            )}

            {showUsedMappingRulesModal && usedMappingRulesFsh && usedMappingRulesName && (
                <FshProfileModal
                    profile={{
                        name: usedMappingRulesName,
                        content: usedMappingRulesFsh,
                        templateId: null,
                        rootCdaType: selectedTemplate.rootCdaType,
                        templateOrigin: "PROJECT",
                        ownershipStatus: "PROJECT",
                        selectionReason: "DIRECT",
                        fhirTransformEligible: false,
                        fhirTransformKind: null,
                        fhirTransformNotice: null,
                    }}
                    contentOverride={usedMappingRulesFsh}
                    titleOverride={`${usedMappingRulesName}.fsh`}
                    fileNameOverride={`${usedMappingRulesName}.fsh`}
                    onClose={() => setShowUsedMappingRulesModal(false)}
                />
            )}
        </div>
    );
}
