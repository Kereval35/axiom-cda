"use client";

import React, { useState, useRef } from "react";
import { GenerationOptions } from "../types/generation";
import { useLanguage } from "./LanguageProvider";

interface GenerationFormProps {
    onGenerate: (options: GenerationOptions) => void;
    loading: boolean;
}

export const GenerationForm: React.FC<GenerationFormProps> = ({ onGenerate, loading }) => {
    const { t } = useLanguage();
    const [bbrInput, setBbrInput] = useState("");
    const [inputMode, setInputMode] = useState<"UPLOAD" | "PASTE" | "URL">("UPLOAD");
    const [uploadedFileName, setUploadedFileName] = useState<string | null>(null);
    const [sushiRepo, setSushiRepo] = useState(true);
    const [emitLogs, setEmitLogs] = useState(true);
    const [projectPlusRequiredIncludes, setProjectPlusRequiredIncludes] = useState(false);
    const [ownedRepositoryPrefixes, setOwnedRepositoryPrefixes] = useState("");
    const [yamlConfig, setYamlConfig] = useState("");
    const fileInputRef = useRef<HTMLInputElement>(null);

    const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            setUploadedFileName(file.name);
            const reader = new FileReader();
            reader.onload = (event) => {
                setBbrInput(event.target?.result as string);
                setInputMode("UPLOAD");
            };
            reader.readAsText(file);
        }
    };

    const handleGenerateClick = () => {
        onGenerate({
            bbr: bbrInput,
            sushiRepo,
            emitIr: true,
            emitLogs,
            yamlConfig: yamlConfig || null,
            projectPlusRequiredIncludes,
            ownedRepositoryPrefixes: ownedRepositoryPrefixes
                .split(",")
                .map((prefix) => prefix.trim())
                .filter(Boolean),
        });
    };

    return (
        <div className="glass rounded-2xl p-6 shadow-2xl transition-all duration-300">
            <div className="flex flex-col gap-6">
                {/* Input Type Toggle */}
                <div className="flex p-1 bg-zinc-200 dark:bg-zinc-900 rounded-xl w-fit border border-zinc-300 dark:border-zinc-700">
                    {[
                        { id: "UPLOAD", label: t.dashboard.inputModes.upload },
                        { id: "PASTE", label: t.dashboard.inputModes.paste },
                        { id: "URL", label: t.dashboard.inputModes.url }
                    ].map((mode) => (
                        <button
                            key={mode.id}
                            onClick={() => setInputMode(mode.id as "UPLOAD" | "PASTE" | "URL")}
                            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${inputMode === mode.id
                                ? "bg-white dark:bg-zinc-700 text-indigo-600 dark:text-indigo-300 shadow-sm"
                                : "text-zinc-700 dark:text-zinc-400 hover:text-indigo-500 shadow-none bg-transparent"
                                }`}
                        >
                            {mode.label}
                        </button>
                    ))}
                </div>

                {/* BBR Input */}
                <div className="space-y-2">
                    <label className="text-sm font-medium !text-[#18181b] dark:!text-zinc-400">
                        {t.dashboard.bbrLabel}
                    </label>

                    {inputMode === "URL" && (
                        <input
                            type="url"
                            placeholder={t.dashboard.urlPlaceholder}
                            value={bbrInput}
                            onChange={(e) => setBbrInput(e.target.value)}
                            className="w-full !bg-[#fafafa] dark:!bg-zinc-900 border border-zinc-300 dark:border-zinc-700 rounded-xl px-4 py-3 !text-[#18181b] dark:!text-zinc-100 placeholder:text-zinc-500 dark:placeholder:text-zinc-500 focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 outline-none transition-all"
                        />
                    )}

                    {inputMode === "PASTE" && (
                        <textarea
                            placeholder={t.dashboard.pastePlaceholder}
                            value={bbrInput}
                            onChange={(e) => setBbrInput(e.target.value)}
                            className="w-full h-48 !bg-[#fafafa] dark:!bg-zinc-900 border border-zinc-300 dark:border-zinc-700 rounded-xl px-4 py-3 !text-[#18181b] dark:!text-zinc-100 placeholder:text-zinc-500 dark:placeholder:text-zinc-500 focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 outline-none transition-all font-mono text-sm resize-none"
                        />
                    )}

                    {inputMode === "UPLOAD" && (
                        <div
                            onClick={() => fileInputRef.current?.click()}
                            className="w-full h-48 flex flex-col items-center justify-center border-2 border-dashed border-zinc-300 dark:border-zinc-700 rounded-xl bg-zinc-50/50 dark:bg-zinc-900/50 hover:bg-zinc-100 dark:hover:bg-zinc-800/50 cursor-pointer transition-all group"
                        >
                            <input
                                type="file"
                                ref={fileInputRef}
                                onChange={handleFileUpload}
                                className="hidden"
                                accept=".xml"
                            />
                            {uploadedFileName ? (
                                <div className="text-center">
                                    <svg className="w-12 h-12 text-indigo-500 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                    </svg>
                                    <p className="text-sm font-medium text-foreground">{uploadedFileName}</p>
                                    <p className="text-xs text-muted mt-1 italic">{t.dashboard.clickToChange}</p>
                                </div>
                            ) : (
                                <div className="text-center">
                                    <svg className="w-12 h-12 text-zinc-400 group-hover:text-indigo-400 mx-auto mb-3 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                                    </svg>
                                    <p className="text-sm font-medium text-foreground">{t.dashboard.clickToUpload}</p>
                                    <p className="text-xs text-muted mt-1">{t.dashboard.acceptsXml}</p>
                                </div>
                            )}
                        </div>
                    )}
                </div>

                {/* Options and YAML */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                    <div className="space-y-4">
                        <h3 className="text-sm font-medium !text-[#18181b] dark:!text-zinc-400">{t.dashboard.options}</h3>
                        <div className="space-y-3">
                            <Checkbox
                                label={t.dashboard.sushiRepoLayout}
                                checked={sushiRepo}
                                onChange={setSushiRepo}
                            />
                            <Checkbox
                                label={t.dashboard.showLogs}
                                checked={emitLogs}
                                onChange={setEmitLogs}
                            />
                            <Checkbox
                                label={
                                    <span className="inline-flex flex-wrap items-center gap-2">
                                        <span>{t.dashboard.projectPlusRequiredIncludes}</span>
                                        <span className="inline-flex items-center rounded-full border border-amber-300 bg-amber-100 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-amber-800 dark:border-amber-800 dark:bg-amber-950/40 dark:text-amber-300">
                                            {t.dashboard.experimental}
                                        </span>
                                    </span>
                                }
                                checked={projectPlusRequiredIncludes}
                                onChange={setProjectPlusRequiredIncludes}
                                hint={t.dashboard.projectPlusRequiredIncludesHint}
                            />
                            {projectPlusRequiredIncludes && (
                                <div className="ml-8 space-y-1">
                                    <label className="text-xs font-medium text-zinc-600 dark:text-zinc-400">
                                        {t.dashboard.ownedRepositoryPrefixes}
                                    </label>
                                    <input
                                        type="text"
                                        value={ownedRepositoryPrefixes}
                                        onChange={(event) => setOwnedRepositoryPrefixes(event.target.value)}
                                        placeholder={t.dashboard.ownedRepositoryPrefixesPlaceholder}
                                        className="w-full !bg-[#fafafa] dark:!bg-zinc-900 border border-zinc-300 dark:border-zinc-700 rounded-lg px-3 py-2 !text-[#18181b] dark:!text-zinc-100 placeholder:text-zinc-500 dark:placeholder:text-zinc-500 focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 outline-none transition-all text-sm"
                                    />
                                    <p className="text-xs text-zinc-500 dark:text-zinc-400">
                                        {t.dashboard.ownedRepositoryPrefixesHint}
                                    </p>
                                </div>
                            )}
                        </div>
                    </div>

                    <div className="space-y-2">
                        <label className="text-sm font-medium !text-[#18181b] dark:!text-zinc-400">
                            {t.dashboard.yamlConfig}
                        </label>
                        <textarea
                            placeholder={t.dashboard.yamlPlaceholder}
                            value={yamlConfig}
                            onChange={(e) => setYamlConfig(e.target.value)}
                            className="w-full h-32 !bg-[#fafafa] dark:!bg-zinc-900 border border-zinc-300 dark:border-zinc-700 rounded-xl px-4 py-3 !text-[#18181b] dark:!text-zinc-100 placeholder:text-zinc-500 dark:placeholder:text-zinc-500 focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 outline-none transition-all font-mono text-sm resize-none"
                        />
                    </div>
                </div>

                {/* Generate Button */}
                <div className="pt-4 border-t border-card-border flex items-center justify-between">
                    <div className="text-xs text-zinc-500 dark:text-zinc-500 italic">
                        {t.dashboard.secureBackend}
                    </div>
                    <button
                        onClick={handleGenerateClick}
                        disabled={loading || !bbrInput}
                        className="relative inline-flex items-center justify-center px-8 py-3 font-semibold text-white transition-all duration-200 bg-gradient-to-r from-indigo-600 to-cyan-600 rounded-xl hover:shadow-[0_0_20px_rgba(79,70,229,0.4)] disabled:opacity-50 disabled:from-zinc-300 disabled:to-zinc-300 dark:disabled:from-zinc-700 dark:disabled:to-zinc-700 disabled:text-zinc-500 disabled:hover:shadow-none"
                    >
                        {loading ? (
                            <span className="flex items-center gap-2">
                                <svg className="animate-spin h-5 w-5 text-current" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                </svg>
                                {t.dashboard.generating}
                            </span>
                        ) : (
                            t.dashboard.generate
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
};

const Checkbox: React.FC<{ label: React.ReactNode; checked: boolean; onChange: (v: boolean) => void; hint?: string }> = ({
    label,
    checked,
    onChange,
    hint,
}) => (
    <label className="flex items-start gap-3 cursor-pointer group">
        <div className="relative flex items-center pt-0.5">
            <input
                type="checkbox"
                checked={checked}
                onChange={(e) => onChange(e.target.checked)}
                className="peer h-5 w-5 rounded border-zinc-300 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-900 text-indigo-600 focus:ring-indigo-500/50 focus:ring-offset-0 accent-indigo-600 transition-all cursor-pointer"
            />
        </div>
        <div className="min-w-0">
            <div className="text-sm !text-[#18181b] dark:!text-zinc-300 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                {label}
            </div>
            {hint && (
                <div className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
                    {hint}
                </div>
            )}
        </div>
    </label>
);
