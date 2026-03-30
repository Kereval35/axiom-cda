'use client';

import { useEffect, useState } from 'react';
import { useLanguage } from '@/components/LanguageProvider';

interface ProgressBarProps {
  slideIndex: number;
  stepIndex: number;
  totalSlides: number;
  maxSteps: number;
  goToSlide: (index: number) => void;
}

export function ProgressBar({ slideIndex, stepIndex, totalSlides, maxSteps, goToSlide }: ProgressBarProps) {
  const [jumpValue, setJumpValue] = useState(String(slideIndex + 1));
  const { t } = useLanguage();

  const progress = ((slideIndex + 1) / totalSlides) * 100;
  const progressLabels = t.presentation.controls.progress;

  useEffect(() => {
    setJumpValue(String(slideIndex + 1));
  }, [slideIndex]);

  const commitJump = (value: string) => {
    const parsed = Number.parseInt(value, 10);
    if (Number.isNaN(parsed)) {
      setJumpValue(String(slideIndex + 1));
      return;
    }

    const clamped = Math.min(Math.max(parsed, 1), totalSlides);
    setJumpValue(String(clamped));
    goToSlide(clamped - 1);
  };

  return (
    <div className="absolute bottom-6 left-0 right-0 z-50">
      {/* Progress bar */}
      <div className="h-1.5 bg-slate-300/50 dark:bg-slate-700/50 backdrop-blur-sm">
        <div
          className="h-full bg-gradient-to-r from-blue-600 via-indigo-600 to-purple-600 transition-all duration-300 ease-out shadow-lg"
          style={{ width: `${progress}%` }}
        />
      </div>

      {/* Optional details */}
      <div className="absolute bottom-4 right-4 bg-white/95 dark:bg-slate-800/95 backdrop-blur-sm px-4 py-2 rounded-lg shadow-lg text-xs font-mono font-semibold text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 flex items-center gap-2">
        <span>{progressLabels.slide}</span>
        <input
          type="number"
          min={1}
          max={totalSlides}
          value={jumpValue}
          onChange={(event) => setJumpValue(event.target.value)}
          onBlur={(event) => commitJump(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              commitJump((event.target as HTMLInputElement).value);
            }
          }}
          className="w-14 px-2 py-1 rounded border border-slate-300 dark:border-slate-600 bg-white/80 dark:bg-slate-900/60 text-center"
        />
        <span>/{totalSlides}</span>
        <span className="text-slate-500 dark:text-slate-400">
          · {progressLabels.step} {stepIndex}/{maxSteps}
        </span>
      </div>
    </div>
  );
}
