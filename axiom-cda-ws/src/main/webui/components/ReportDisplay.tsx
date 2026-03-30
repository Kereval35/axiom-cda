import React from "react";
import { GenerationReport as IReport } from "../types/generation";

interface ReportDisplayProps {
    report: IReport;
}

export const ReportDisplay: React.FC<ReportDisplayProps> = ({ report }) => {
    return (
        <div className="glass rounded-2xl overflow-hidden transition-all duration-300">
            <div className="px-6 py-4 border-b border-card-border bg-zinc-50 dark:bg-zinc-900/50 flex items-center justify-between">
                <h3 className="font-semibold text-foreground flex items-center gap-2">
                    <svg className="w-4 h-4 text-indigo-500 dark:text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    Generation Report
                </h3>
                <div className="flex gap-4 text-xs font-mono">
                    <span className="text-emerald-600 dark:text-emerald-400 font-bold">OK: {report.templatesOk}</span>
                    <span className="text-amber-600 dark:text-amber-400 font-bold">Warn: {report.warnings.length}</span>
                    <span className="text-rose-600 dark:text-rose-400 font-bold">Err: {report.errors.length}</span>
                </div>
            </div>
            <div className="p-6">
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
                    {[
                        { label: "Considered", val: report.templatesConsidered, color: "text-foreground" },
                        { label: "Generated", val: report.templatesGenerated, color: "text-indigo-600 dark:text-indigo-400" },
                        { label: "Skipped", val: report.templatesSkipped, color: "text-zinc-600 dark:text-zinc-400" },
                        { label: "Unmapped", val: report.unmappedElements, color: "text-amber-600 dark:text-amber-500" },
                    ].map((stat, i) => (
                        <div key={i} className="bg-white dark:bg-zinc-900 p-4 rounded-xl border border-card-border shadow-sm">
                            <div className="text-xs text-zinc-500 dark:text-zinc-500 font-medium uppercase tracking-wider">{stat.label}</div>
                            <div className={`text-2xl font-bold mt-1 ${stat.color}`}>{stat.val}</div>
                        </div>
                    ))}
                </div>

                {(report.errors.length > 0 || report.warnings.length > 0) && (
                    <div className="space-y-4">
                        <h4 className="text-sm font-semibold text-zinc-600 dark:text-zinc-400 uppercase tracking-widest px-1">Diagnostics</h4>
                        <div className="max-h-96 overflow-y-auto bg-zinc-50 dark:bg-zinc-950 rounded-xl p-4 font-mono text-xs border border-card-border space-y-2">
                            {report.errors.map((msg, i) => (
                                <div key={`err-${i}`} className="text-rose-600 dark:text-rose-400 flex gap-3">
                                    <span className="shrink-0 font-bold opacity-50 uppercase">Error</span>
                                    <span>{msg}</span>
                                </div>
                            ))}
                            {report.warnings.map((msg, i) => (
                                <div key={`warn-${i}`} className="text-amber-600 dark:text-amber-400 flex gap-3">
                                    <span className="shrink-0 font-bold opacity-50 uppercase">Warn</span>
                                    <span>{msg}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};
