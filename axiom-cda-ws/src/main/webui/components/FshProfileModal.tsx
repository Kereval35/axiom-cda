"use client";

import React from "react";
import { formatFsh } from "../utils/fshFormatter";
import { FshHighlighter } from "./FshHighlighter";
import { FshProfile } from "../types/generation";
import { useLanguage } from "./LanguageProvider";

interface FshProfileModalProps {
    profile: FshProfile;
    contentOverride?: string;
    titleOverride?: string;
    fileNameOverride?: string;
    convertLabelOverride?: string;
    onConvert?: () => void;
    onClose: () => void;
}

export const FshProfileModal: React.FC<FshProfileModalProps> = ({
    profile,
    contentOverride,
    titleOverride,
    fileNameOverride,
    convertLabelOverride,
    onConvert,
    onClose,
}) => {
    const [copied, setCopied] = React.useState(false);
    const [formatted, setFormatted] = React.useState(false);
    const [isFormatting, setIsFormatting] = React.useState(false);
    const { t } = useLanguage();

    const toggleFormat = () => {
        if (isFormatting) return;
        setIsFormatting(true);
        setTimeout(() => {
            setFormatted(!formatted);
            setIsFormatting(false);
        }, 50);
    };

    const rawContent = contentOverride ?? profile.content;
    const modalTitle = titleOverride ?? `${profile.name}.fsh`;
    const downloadFileName = fileNameOverride ?? `${profile.name}.fsh`;
    const displayContent = formatted ? formatFsh(rawContent) : rawContent;

    const copyToClipboard = () => {
        navigator.clipboard.writeText(displayContent);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    const downloadFsh = () => {
        const blob = new Blob([displayContent], { type: "text/plain" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = downloadFileName;
        link.click();
        URL.revokeObjectURL(url);
    };

    return (
        <div 
            className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4 z-50"
            onClick={onClose}
        >
            <div 
                className="bg-white dark:bg-zinc-900 rounded-2xl shadow-2xl max-w-4xl w-full max-h-[80vh] overflow-hidden"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="px-6 py-4 border-b border-zinc-200 dark:border-zinc-700 flex items-center justify-between bg-zinc-50 dark:bg-zinc-800">
                    <div>
                        <h3 className="text-lg font-bold text-zinc-800 dark:text-zinc-100 font-mono">{modalTitle}</h3>
                        {profile.fhirTransformKind === "observation" && (
                            <div className="mt-2 flex items-center gap-2">
                                <span className="inline-flex items-center rounded-full bg-cyan-100 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-cyan-700 dark:bg-cyan-950/40 dark:text-cyan-300">
                                    FHIR Ready
                                </span>
                                <span className="text-xs text-zinc-600 dark:text-zinc-300">
                                    {profile.fhirTransformNotice ?? t.dashboard.fhirEligible}
                                </span>
                            </div>
                        )}
                    </div>
                    <div className="flex items-center gap-2">
                        <button
                            onClick={toggleFormat}
                            disabled={isFormatting}
                            className={`flex items-center gap-2 px-3 py-1.5 text-sm font-medium rounded-lg transition-colors ${
                                formatted
                                    ? 'bg-indigo-100 dark:bg-indigo-900/30 text-indigo-700 dark:text-indigo-300'
                                    : 'bg-zinc-100 dark:bg-zinc-700 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-200 dark:hover:bg-zinc-600'
                            }`}
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 10h16M4 14h16M4 18h16" />
                            </svg>
                            Format
                        </button>
                        <button
                            onClick={onClose}
                            className="p-2 hover:bg-zinc-200 dark:hover:bg-zinc-700 rounded-lg transition-colors"
                        >
                            <svg className="w-5 h-5 text-zinc-600 dark:text-zinc-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                            </svg>
                        </button>
                    </div>
                </div>

                <div className="p-6 overflow-y-auto max-h-[60vh]">
                    {formatted ? (
                        <FshHighlighter code={displayContent} />
                    ) : (
                        <pre className="bg-zinc-50 dark:bg-zinc-950 rounded-lg overflow-x-auto p-4 font-mono text-sm text-zinc-800 dark:text-zinc-300 whitespace-pre">
                            {displayContent}
                        </pre>
                    )}
                </div>

                <div className="px-6 py-4 border-t border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800 flex justify-end gap-3">
                    {profile.fhirTransformEligible && onConvert && (
                        <div className="flex flex-wrap items-center justify-end gap-3">
                            {profile.fhirTransformKind === "observation" && (
                                <span className="max-w-xs text-right text-xs text-zinc-500 dark:text-zinc-400">
                                    BETA: This profiles could be directly converted to FHIR using builting mapper
                                </span>
                            )}
                            <button
                                onClick={onConvert}
                                className="flex items-center gap-2 px-4 py-2 bg-cyan-600 hover:bg-cyan-500 text-white rounded-lg font-medium transition-colors shadow-lg shadow-cyan-600/20"
                            >
                                {convertLabelOverride ?? t.dashboard.convertToFhir}
                            </button>
                        </div>
                    )}
                    <button
                        onClick={copyToClipboard}
                        className="flex items-center gap-2 px-4 py-2 bg-zinc-200 dark:bg-zinc-700 hover:bg-zinc-300 dark:hover:bg-zinc-600 text-zinc-800 dark:text-zinc-100 rounded-lg font-medium transition-colors"
                    >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                        </svg>
                        {copied ? "Copied!" : "Copy"}
                    </button>
                    <button
                        onClick={downloadFsh}
                        className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg font-medium transition-colors shadow-lg shadow-indigo-600/20"
                    >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                        </svg>
                        Download
                    </button>
                </div>
            </div>
        </div>
    );
};
