"use client";

import { Deck } from '@/components/presentation/Deck';
import { getSlides } from '@/components/presentation/data/slides';
import { useLanguage } from '@/components/LanguageProvider';
import { useMemo, useState } from 'react';

export default function Home() {
  const { language, t } = useLanguage();
  const [mode, setMode] = useState<'minimal' | 'full'>('full');
  const controls = t.presentation.controls;

  const slides = useMemo(() => {
    const allSlides = getSlides(language);
    if (mode === 'full') {
      return allSlides;
    }

    const minimalIds = new Set([
      'slide-1',
      'slide-2',
      'slide-3',
      'slide-4',
      'slide-5',
      'slide-6',
      'slide-15',
      'slide-18',
      'slide-20',
      'slide-21',
      'slide-22',
    ]);

    return allSlides.filter((slide) => minimalIds.has(slide.id));
  }, [language, mode]);

  const deckKey = `${language}-${mode}`;

  return (
    <div className="w-full h-full flex flex-col items-center gap-6">
      <div className="w-full max-w-[95vw] flex justify-end">
        <div className="inline-flex rounded-full border border-slate-200 dark:border-slate-700 bg-white/80 dark:bg-slate-900/50 p-1 shadow-sm">
          <button
            type="button"
            onClick={() => setMode('minimal')}
            className={`px-4 py-1.5 rounded-full text-xs font-semibold transition ${mode === 'minimal' ? 'bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900' : 'text-slate-600 dark:text-slate-300'}`}
          >
            {controls.modes.minimal}
          </button>
          <button
            type="button"
            onClick={() => setMode('full')}
            className={`px-4 py-1.5 rounded-full text-xs font-semibold transition ${mode === 'full' ? 'bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900' : 'text-slate-600 dark:text-slate-300'}`}
          >
            {controls.modes.full}
          </button>
        </div>
      </div>
      <Deck key={deckKey} slides={slides} />
    </div>
  );
}
