"use client";

import React from "react";
import { useLanguage } from "./LanguageProvider";

export const LanguageToggle: React.FC = () => {
  const { language, toggleLanguage, mounted } = useLanguage();

  if (!mounted) {
    return (
      <div className="p-2 rounded-xl bg-zinc-200 dark:bg-zinc-900 border border-zinc-300 dark:border-zinc-800 w-9 h-9" />
    );
  }

  return (
    <button
      onClick={toggleLanguage}
      className="p-2 rounded-xl bg-zinc-200 dark:bg-zinc-900 border border-zinc-300 dark:border-zinc-800 text-zinc-700 dark:text-zinc-400 hover:text-indigo-500 transition-all shadow-sm"
      aria-label="Toggle Language"
      title={language === "en" ? "Switch to French" : "Passer à l'anglais"}
    >
      <span className="font-semibold text-sm w-5 h-5 flex items-center justify-center">
        {language === "en" ? "FR" : "EN"}
      </span>
    </button>
  );
};
