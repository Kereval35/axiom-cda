"use client";

import React, { useState } from "react";
import { IRTemplate, IRTemplateElement, IRTemplateInclude, IRTemplateInvariant } from "../types/generation";

interface IRViewerProps {
    templates: IRTemplate[];
}

export const IRViewer: React.FC<IRViewerProps> = ({ templates }) => {
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

    return (
        <div className="glass rounded-2xl p-6 shadow-2xl">
            <div className="mb-4">
                <h2 className="text-xl font-semibold !text-[#18181b] dark:!text-zinc-100 mb-2">
                    Intermediate Representation (IR)
                </h2>
                <p className="text-sm text-zinc-600 dark:text-zinc-400">
                    {templates.length} template{templates.length !== 1 ? "s" : ""} generated
                </p>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
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
                            <button
                                key={template.id}
                                onClick={() => setSelectedTemplate(template)}
                                className={`w-full text-left p-3 rounded-xl transition-all ${
                                    selectedTemplate?.id === template.id
                                        ? "bg-indigo-500/10 border-2 border-indigo-500/50 dark:bg-indigo-500/20"
                                        : "bg-zinc-100/50 dark:bg-zinc-800/50 border border-zinc-300 dark:border-zinc-700 hover:bg-zinc-200/50 dark:hover:bg-zinc-700/50"
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
                                    {template.elements && template.elements.length > 0 && (
                                        <span className="text-xs px-2 py-0.5 bg-zinc-200 dark:bg-zinc-700 text-zinc-700 dark:text-zinc-300 rounded">
                                            {template.elements.length} elements
                                        </span>
                                    )}
                                </div>
                            </button>
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
                                    <div className="px-3 py-2 bg-indigo-500/10 border border-indigo-500/30 rounded-lg text-sm !text-[#18181b] dark:!text-zinc-100">
                                        {selectedTemplate.rootCdaType}
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
            </div>
        </div>
    );
};
