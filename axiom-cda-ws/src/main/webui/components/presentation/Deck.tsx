'use client';

import { useDeckNavigation } from '@/hooks/useDeckNavigation';
import { SlideRenderer } from './SlideRenderer';
import { ProgressBar } from './ProgressBar';
// import { Footer } from './Footer';
// import { ThemeToggle } from './ThemeToggle';
import type { Slide } from '@/types/presentation/types';

interface DeckProps {
  slides: Slide[];
}

export function Deck({ slides }: DeckProps) {
  const { slideIndex, stepIndex, currentSlide, totalSlides, goToSlide } = useDeckNavigation(slides);

  if (!currentSlide) {
    return (
      <div className="w-screen h-screen flex items-center justify-center">
        <p className="text-xl text-slate-500">Aucune slide disponible</p>
      </div>
    );
  }

  return (
    <div className="deck-active w-full h-[calc(100vh-280px)] overflow-hidden relative bg-zinc-50 dark:bg-zinc-950 rounded-[2.5rem] border border-card-border shadow-2xl mx-auto max-w-[95vw] transition-all duration-500">
      {/* Subtle texture background */}
      <div className="absolute inset-0 opacity-40 dark:opacity-20 bg-texture"></div>

      {/* Decorative background patterns */}
      <div className="absolute inset-0 opacity-20 dark:opacity-10">
        <div className="absolute top-0 left-0 w-[600px] h-[600px] bg-gradient-to-br from-indigo-500/30 to-transparent rounded-full blur-3xl"></div>
        <div className="absolute bottom-0 right-0 w-[600px] h-[600px] bg-gradient-to-tl from-cyan-500/30 to-transparent rounded-full blur-3xl"></div>
      </div>

      {/* Slide content */}
      <div className="relative w-full h-full">
        <SlideRenderer slide={currentSlide} stepIndex={stepIndex} />
      </div>

      {/* Progress bar */}
      <ProgressBar
        slideIndex={slideIndex}
        stepIndex={stepIndex}
        totalSlides={totalSlides}
        maxSteps={currentSlide.stepsCount}
        goToSlide={goToSlide}
      />

      {/* Footer is now global in layout */}

      {/* Navigation hints (subtle, bottom-left) */}
      <div className="absolute bottom-10 left-6 text-xs text-slate-600 dark:text-slate-400 font-mono hidden lg:block z-50">
        <div className="bg-white/90 dark:bg-slate-800/90 backdrop-blur-sm px-3 py-2 rounded-lg shadow-lg border border-slate-200 dark:border-slate-700 space-y-1">
          <div>← → : Navigation</div>
          <div>Space : Suivant</div>
          <div>F : Plein écran</div>
        </div>
      </div>
    </div>
  );
}
