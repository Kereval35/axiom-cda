"use client";

import React, { createContext, useContext, useState, useSyncExternalStore } from "react";
import { translations, Language, TranslationKey } from "../translations/i18n";

interface LanguageContextType {
  language: Language;
  toggleLanguage: () => void;
  setLanguage: (lang: Language) => void;
  t: TranslationKey;
  mounted: boolean;
}

const LanguageContext = createContext<LanguageContextType | undefined>(undefined);

export const LanguageProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const mounted = useSyncExternalStore(
    () => () => {},
    () => true,
    () => false
  );
  const [language, setLanguage] = useState<Language>(() => {
    if (typeof window === "undefined") {
      return "en";
    }

    const savedLanguage = localStorage.getItem("language") as Language | null;
    if (savedLanguage === "en" || savedLanguage === "fr") {
      return savedLanguage;
    }

    return navigator.language.split("-")[0] === "fr" ? "fr" : "en";
  });

  const toggleLanguage = () => {
    const newLanguage = language === "en" ? "fr" : "en";
    setLanguage(newLanguage);
    localStorage.setItem("language", newLanguage);
  };

  const setLanguageHandler = (lang: Language) => {
    setLanguage(lang);
    localStorage.setItem("language", lang);
  };

  return (
    <LanguageContext.Provider value={{ language, toggleLanguage, setLanguage: setLanguageHandler, t: translations[language], mounted }}>
      {children}
    </LanguageContext.Provider>
  );
};

export const useLanguage = () => {
  const context = useContext(LanguageContext);
  if (context === undefined) {
    throw new Error("useLanguage must be used within LanguageProvider");
  }
  return context;
};
