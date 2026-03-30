"use client";

import React from "react";
import { GenerationResult } from "../types/generation";
import { useLanguage } from "./LanguageProvider";

interface ResultCardProps {
    result: GenerationResult;
}

export const ResultCard: React.FC<ResultCardProps> = ({ result }) => {
    const { t } = useLanguage();

    const downloadZip = () => {
        const link = document.createElement("a");
        link.href = `data:application/zip;base64,${result.zipBase64}`;
        link.download = "axiom-cda-fsh.zip";
        link.click();
    };

    return (
        <div className="bg-indigo-500/10 dark:bg-indigo-500/10 border border-indigo-500/20 dark:border-indigo-500/30 rounded-2xl p-6 flex flex-col md:flex-row items-center justify-between gap-6 transition-all duration-300">
            <div>
                <h2 className="text-xl font-bold text-indigo-600 dark:text-indigo-400">{t.dashboard.generationComplete}</h2>
                <p className="text-zinc-600 dark:text-zinc-400 mt-1">
                    {t.dashboard.generated.replace("{profiles}", result.report.profilesGenerated.toString()).replace("{invariants}", result.report.invariantsGenerated.toString())}
                </p>
            </div>
            <button
                onClick={downloadZip}
                className="flex items-center gap-2 px-6 py-3 bg-indigo-600 hover:bg-indigo-500 dark:bg-indigo-600 dark:hover:bg-indigo-500 text-white rounded-xl font-bold transition-all shadow-lg shadow-indigo-600/20 whitespace-nowrap"
            >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                </svg>
                {t.dashboard.downloadZip}
            </button>
        </div>
    );
};
