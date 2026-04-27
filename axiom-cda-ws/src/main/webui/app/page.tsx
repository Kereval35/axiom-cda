"use client";

import React from "react";
import { GenerationForm } from "../components/GenerationForm";
import { ResultCard } from "../components/ResultCard";
import { ReportDisplay } from "../components/ReportDisplay";
import { FshProfilesViewer } from "../components/FshProfilesViewer";
import { IRViewer } from "../components/IRViewer";
import { useGeneration } from "../hooks/useGeneration";
import { useLanguage } from "../components/LanguageProvider";

export default function AxiomCdaPage() {
  const { loading, result, error, generate } = useGeneration();
  const { t } = useLanguage();

  return (
    <div className="selection:bg-indigo-500/30 max-w-5xl mx-auto px-6 py-4">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        <main className="lg:col-span-12 space-y-6">
          <GenerationForm onGenerate={generate} loading={loading} />

          {/* Error Message */}
          {error && (
            <div className="bg-red-500/10 border border-red-500/20 rounded-2xl p-4 text-red-600 dark:text-red-400 text-sm animate-in fade-in duration-300">
              <span className="font-bold">{t.dashboard.error}:</span> {error}
            </div>
          )}

          {/* Result Section */}
          {result && (
            <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
              <ResultCard result={result} />
              {result.irTemplates && result.irTemplates.length > 0 && (
                <IRViewer templates={result.irTemplates} />
              )}
              <FshProfilesViewer profiles={result.profiles} irTemplates={result.irTemplates} />
              <ReportDisplay report={result.report} />
            </div>
          )}
        </main>
      </div>

      {/* Background decoration */}
      <div className="fixed top-0 left-0 w-full h-full -z-10 overflow-hidden pointer-events-none">
        <div className="absolute top-[-10%] right-[-5%] w-[40%] h-[40%] bg-indigo-600/5 dark:bg-indigo-600/10 blur-[120px] rounded-full transition-colors"></div>
        <div className="absolute bottom-[-5%] left-[-5%] w-[30%] h-[30%] bg-cyan-600/5 dark:bg-cyan-600/10 blur-[100px] rounded-full transition-colors"></div>
      </div>
    </div>
  );
}
