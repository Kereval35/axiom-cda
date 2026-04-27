"use client";

import React from "react";
import { createPortal } from "react-dom";
import { FshProfile, IRTemplate } from "../types/generation";
import { FshProfileModal } from "./FshProfileModal";
import { saveFhirConversionSession } from "../utils/fhirConversionSession";
import { withBasePath } from "../utils/config";
import { getReadableProfileName, sortProfilesForDisplay } from "../utils/profileDisplay";

interface FshProfilesViewerProps {
    profiles: FshProfile[];
    irTemplates: IRTemplate[];
}

export const FshProfilesViewer: React.FC<FshProfilesViewerProps> = ({ profiles, irTemplates }) => {
    const [isCollapsed, setIsCollapsed] = React.useState(false);
    const [showMore, setShowMore] = React.useState(false);
    const [selectedProfile, setSelectedProfile] = React.useState<FshProfile | null>(null);
    const profilesPerRow = 4;
    const sortedProfiles = React.useMemo(() => sortProfilesForDisplay(profiles), [profiles]);

    const visibleProfiles = showMore ? sortedProfiles : sortedProfiles.slice(0, profilesPerRow * 2);
    const hasMoreProfiles = sortedProfiles.length > profilesPerRow * 2;

    const handleToggle = () => {
        setIsCollapsed(!isCollapsed);
    };

    const handleShowMore = () => {
        setShowMore(true);
    };

    const openProfile = (profile: FshProfile) => setSelectedProfile(profile);

    const handleConvert = (profile: FshProfile) => {
        if (!profile.templateId) {
            return;
        }
        saveFhirConversionSession({
            profiles,
            irTemplates,
            selectedProfileName: profile.name,
            selectedTemplateId: profile.templateId,
        });
        window.location.assign(withBasePath("convert/fhir/"));
    };

    if (profiles.length === 0) {
        return null;
    }

    return (
        <>
            <div className="glass rounded-2xl overflow-hidden transition-all duration-300 animate-in fade-in slide-in-from-bottom-4 duration-500">
                <div 
                    className="px-6 py-4 border-b border-card-border bg-zinc-50 dark:bg-zinc-900/50 flex items-center justify-between cursor-pointer hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
                    onClick={handleToggle}
                >
                    <h3 className="font-semibold text-foreground flex items-center gap-2">
                        <svg className="w-4 h-4 text-indigo-500 dark:text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                        Generated FSH Profiles ({profiles.length})
                    </h3>
                    <svg 
                        className={`w-5 h-5 text-zinc-500 dark:text-zinc-400 transition-transform duration-300 ${isCollapsed ? '-rotate-90' : ''}`}
                        fill="none" 
                        stroke="currentColor" 
                        viewBox="0 0 24 24"
                    >
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                    </svg>
                </div>

                {!isCollapsed && (
                    <div className="p-6">
                        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
                            {visibleProfiles.map((profile) => (
                                (() => {
                                    const readableName = getReadableProfileName(profile.name);
                                    return (
                                <button
                                    key={profile.name}
                                    onClick={() => openProfile(profile)}
                                    title={profile.name}
                                    className="px-4 py-3 bg-white dark:bg-zinc-900 border border-card-border hover:border-indigo-500/50 dark:hover:border-indigo-500/50 hover:bg-indigo-50 dark:hover:bg-indigo-950/20 hover:shadow-md transition-all rounded-lg text-left group"
                                >
                                    <div
                                        className="text-sm font-medium text-zinc-800 dark:text-zinc-200 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors truncate"
                                        title={profile.name}
                                    >
                                        {readableName}
                                    </div>
                                    <div className="mt-1 flex items-center gap-2">
                                        <div className="text-xs text-zinc-500 dark:text-zinc-400 group-hover:text-indigo-500/70 dark:group-hover:text-indigo-400/70 transition-colors">
                                            {profile.content.split('\n').length} lines
                                        </div>
                                        {profile.fhirTransformEligible && (
                                            <span className="inline-flex items-center rounded-full bg-cyan-100 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-cyan-700 dark:bg-cyan-950/40 dark:text-cyan-300">
                                                FHIR
                                            </span>
                                        )}
                                    </div>
                                </button>
                                    );
                                })()
                            ))}
                        </div>

                        {!showMore && hasMoreProfiles && (
                            <div className="mt-4 text-center">
                                <button
                                    onClick={handleShowMore}
                                    className="inline-flex items-center gap-2 px-4 py-2 bg-zinc-100 dark:bg-zinc-800 hover:bg-zinc-200 dark:hover:bg-zinc-700 text-zinc-700 dark:text-zinc-300 rounded-lg font-medium transition-colors"
                                >
                                    <span>Show {sortedProfiles.length - profilesPerRow * 2} more profiles</span>
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                                    </svg>
                                </button>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {selectedProfile && createPortal(
                <FshProfileModal
                    profile={selectedProfile}
                    onConvert={() => handleConvert(selectedProfile)}
                    onClose={() => setSelectedProfile(null)}
                />,
                document.body
            )}
        </>
    );
};
