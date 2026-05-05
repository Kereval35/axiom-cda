"use client";

import React, { useState } from "react";
import { IRTemplate, IRTemplateElement, IRTemplateInclude, IRTemplateInvariant } from "../types/generation";

interface IRViewerProps {
    templates: IRTemplate[];
}

export const IRViewer: React.FC<IRViewerProps> = ({ templates }) => {
    const [isCollapsed, setIsCollapsed] = useState(true);
    const [selectedTemplate, setSelectedTemplate] = useState<IRTemplate | null>(
        templates.length > 0 ? templates[0] : null
    );
    const [searchTerm, setSearchTerm] = useState("");

    if (!templates || templates.length === 0) {
        return null;
    }

    const filteredTemplates = templates.filter(
        (t) =>
            t.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
            t.displayName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            t.name?.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const getTemplateTone = (origin: string) => {
        if (origin === "PROJECT") {
            return {
                selected: "bg-blue-500/10 border-2 border-blue-500/60 dark:bg-blue-500/20 shadow-[0_0_20px_rgba(59,130,246,0.18)]",
                idle: "bg-blue-50/60 dark:bg-blue-950/10 border border-blue-300/60 dark:border-blue-700/40 hover:bg-blue-100/70 dark:hover:bg-blue-900/20",
                badge: "bg-blue-100 text-blue-700 dark:bg-blue-950/50 dark:text-blue-300",
                label: "Project",
            };
        }
        if (origin === "REQUIRED_INCLUDE") {
            return {
                selected: "bg-zinc-200/70 dark:bg-zinc-800 border-2 border-zinc-400/70 dark:border-zinc-600",
                idle: "bg-zinc-100/70 dark:bg-zinc-800/50 border border-zinc-400/70 dark:border-zinc-600 hover:bg-zinc-200/70 dark:hover:bg-zinc-700/70",
                badge: "bg-zinc-200 text-zinc-700 dark:bg-zinc-700 dark:text-zinc-200",
                label: "Required include",
            };
        }
        return {
            selected: "bg-indigo-500/10 border-2 border-indigo-500/50 dark:bg-indigo-500/20",
            idle: "bg-zinc-100/50 dark:bg-zinc-800/50 border border-zinc-300 dark:border-zinc-700 hover:bg-zinc-200/50 dark:hover:bg-zinc-700/50",
            badge: "bg-indigo-100 text-indigo-700 dark:bg-indigo-950/50 dark:text-indigo-300",
            label: "Other",
        };
    };

    return (
        <div className="glass rounded-2xl p-6 shadow-2xl">
            <div
                className={`${isCollapsed ? "" : "mb-4"} flex cursor-pointer items-center justify-between gap-4`}
                onClick={() => setIsCollapsed((collapsed) => !collapsed)}
                role="button"
                tabIndex={0}
                onKeyDown={(event) => {
                    if (event.key === "Enter" || event.key === " ") {
                        event.preventDefault();
                        setIsCollapsed((collapsed) => !collapsed);
                    }
                }}
            >
                <div>
                    <h2 className="text-xl font-semibold !text-[#18181b] dark:!text-zinc-100 mb-2">
                        Intermediate Representation (IR)
                    </h2>
                    <p className="text-sm text-zinc-600 dark:text-zinc-400">
                        {templates.length} template{templates.length !== 1 ? "s" : ""} generated
                    </p>
                </div>
                <svg
                    className={`h-5 w-5 text-zinc-500 transition-transform dark:text-zinc-400 ${isCollapsed ? "-rotate-90" : ""}`}
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                </svg>
            </div>

            {!isCollapsed && <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
                {/* Template List */}
                <div className="lg:col-span-4 space-y-3">
                    {/* Search */}
                    <input
                        type="text"
                        placeholder="Search templates..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="w-full !bg-[#fafafa] dark:!bg-zinc-900 border border-zinc-300 dark:border-zinc-700 rounded-xl px-4 py-2 !text-[#18181b] dark:!text-zinc-100 placeholder:text-zinc-500 dark:placeholder:text-zinc-500 focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 outline-none transition-all text-sm"
                    />

                    {/* Template List */}
                    <div className="space-y-2 max-h-[500px] overflow-y-auto pr-2">
                        {filteredTemplates.map((template) => (
                            (() => {
                                const tone = getTemplateTone(template.origin);
                                return (
                            <button
                                key={template.id}
                                onClick={() => setSelectedTemplate(template)}
                                className={`w-full text-left p-3 rounded-xl transition-all ${
                                    selectedTemplate?.id === template.id
                                        ? tone.selected
                                        : tone.idle
                                }`}
                            >
                                <div className="font-medium text-sm !text-[#18181b] dark:!text-zinc-100 truncate">
                                    {template.displayName || template.name || template.id}
                                </div>
                                <div className="text-xs text-zinc-500 dark:text-zinc-400 truncate mt-1">
                                    {template.id}
                                </div>
                                <div className="flex gap-2 mt-2">
                                    <span className="text-xs px-2 py-0.5 bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 rounded">
                                        {template.rootCdaType}
                                    </span>
                                    <span className={`text-xs px-2 py-0.5 rounded ${tone.badge}`}>
                                        {tone.label}
                                    </span>
                                    {template.elements && template.elements.length > 0 && (
                                        <span className="text-xs px-2 py-0.5 bg-zinc-200 dark:bg-zinc-700 text-zinc-700 dark:text-zinc-300 rounded">
                                            {template.elements.length} elements
                                        </span>
                                    )}
                                </div>
                            </button>
                                );
                            })()
                        ))}
                    </div>
                </div>

                {/* Template Details */}
                <div className="lg:col-span-8">
                    {selectedTemplate ? (
                        <div className="bg-zinc-50/50 dark:bg-zinc-900/50 border border-zinc-300 dark:border-zinc-700 rounded-xl p-4 max-h-[500px] overflow-y-auto">
                            <div className="space-y-4">
                                {/* Header */}
                                <div>
                                    <h3 className="text-lg font-semibold !text-[#18181b] dark:!text-zinc-100">
                                        {selectedTemplate.displayName || selectedTemplate.name}
                                    </h3>
                                    <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
                                        {selectedTemplate.id}
                                    </p>
                                    {selectedTemplate.description && (
                                        <p className="text-sm text-zinc-600 dark:text-zinc-300 mt-2">
                                            {selectedTemplate.description}
                                        </p>
                                    )}
                                </div>

                                {/* Root Type */}
                                <div>
                                    <div className="text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase mb-2">
                                        Root CDA Type
                                    </div>
                                    <div className="flex flex-wrap gap-2">
                                        <div className="px-3 py-2 bg-indigo-500/10 border border-indigo-500/30 rounded-lg text-sm !text-[#18181b] dark:!text-zinc-100">
                                            {selectedTemplate.rootCdaType}
                                        </div>
                                        <div className={`px-3 py-2 rounded-lg text-sm ${
                                            selectedTemplate.origin === "PROJECT"
                                                ? "bg-blue-100 text-blue-700 border border-blue-300 dark:bg-blue-950/40 dark:text-blue-300 dark:border-blue-700/40"
                                                : selectedTemplate.origin === "REQUIRED_INCLUDE"
                                                    ? "bg-zinc-200 text-zinc-700 border border-zinc-400 dark:bg-zinc-800 dark:text-zinc-200 dark:border-zinc-600"
                                                    : "bg-zinc-200 text-zinc-700 border border-zinc-300 dark:bg-zinc-800 dark:text-zinc-300 dark:border-zinc-700"
                                        }`}>
                                            {selectedTemplate.origin === "PROJECT"
                                                ? "Project"
                                                : selectedTemplate.origin === "REQUIRED_INCLUDE"
                                                    ? "Required include"
                                                    : "Other"}
                                        </div>
                                    </div>
                                </div>

                                {/* Elements */}
                                {selectedTemplate.elements && selectedTemplate.elements.length > 0 && (
                                    <div>
                                        <div className="text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase mb-2">
                                            Elements ({selectedTemplate.elements.length})
                                        </div>
                                        <div className="space-y-1 max-h-[200px] overflow-y-auto">
                                            {selectedTemplate.elements.slice(0, 20).map((elem: IRTemplateElement, idx: number) => (
                                                <div
                                                    key={idx}
                                                    className="px-3 py-2 bg-zinc-100 dark:bg-zinc-800 rounded-lg text-xs !text-[#18181b] dark:!text-zinc-100"
                                                >
                                                    <span className="font-mono">{elem.path || elem.name || `Element ${idx + 1}`}</span>
                                                </div>
                                            ))}
                                            {selectedTemplate.elements.length > 20 && (
                                                <div className="text-xs text-zinc-500 dark:text-zinc-400 italic px-3 py-2">
                                                    ... and {selectedTemplate.elements.length - 20} more
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                )}

                                {/* Includes */}
                                {selectedTemplate.includes && selectedTemplate.includes.length > 0 && (
                                    <div>
                                        <div className="text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase mb-2">
                                            Includes ({selectedTemplate.includes.length})
                                        </div>
                                        <div className="space-y-1">
                                            {selectedTemplate.includes.map((include: IRTemplateInclude, idx: number) => (
                                                <div
                                                    key={idx}
                                                    className="px-3 py-2 bg-cyan-500/10 border border-cyan-500/30 rounded-lg text-sm !text-[#18181b] dark:!text-zinc-100"
                                                >
                                                    {include.templateId || include.id || `Include ${idx + 1}`}
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}

                                {/* Invariants */}
                                {selectedTemplate.invariants && selectedTemplate.invariants.length > 0 && (
                                    <div>
                                        <div className="text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase mb-2">
                                            Invariants ({selectedTemplate.invariants.length})
                                        </div>
                                        <div className="space-y-1">
                                            {selectedTemplate.invariants.map((inv: IRTemplateInvariant, idx: number) => (
                                                <div
                                                    key={idx}
                                                    className="px-3 py-2 bg-amber-500/10 border border-amber-500/30 rounded-lg text-sm"
                                                >
                                                    <div className="font-medium !text-[#18181b] dark:!text-zinc-100">
                                                        {inv.name || inv.id || `Invariant ${idx + 1}`}
                                                    </div>
                                                    {inv.description && (
                                                        <div className="text-xs text-zinc-600 dark:text-zinc-300 mt-1">
                                                            {inv.description}
                                                        </div>
                                                    )}
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    ) : (
                        <div className="flex items-center justify-center h-[500px] text-zinc-500 dark:text-zinc-400">
                            Select a template to view details
                        </div>
                    )}
                </div>
            </div>}
        </div>
    );
};
