'use client';

import { Fragment } from './Fragment';
import type { Slide } from '@/types/presentation/types';

interface SlideRendererProps {
  slide: Slide;
  stepIndex: number;
}

export function SlideRenderer({ slide, stepIndex }: SlideRendererProps) {
  return (
    <div className="w-full h-full flex items-start justify-center overflow-y-auto p-8 pb-32">
      {/* Centered container with max width */}
      <div className="w-full max-w-5xl my-auto">
        {/* Title - Centered */}
        <div className="mb-12 text-center">
          <h1 className="text-6xl font-bold text-slate-900 dark:text-slate-100 mb-4 leading-tight">
            {slide.title}
          </h1>
          {slide.subtitle && (
            <p className="text-2xl text-slate-600 dark:text-slate-400 font-light">
              {slide.subtitle}
            </p>
          )}
        </div>

        {/* Always visible content */}
        {slide.content.length > 0 && (
          <div className="space-y-4 mb-8">
            {slide.content.map((item, index) => (
              <div key={index} className="text-xl text-slate-700 dark:text-slate-300 leading-relaxed">
                {item}
              </div>
            ))}
          </div>
        )}

        {/* Fragments (revealed progressively) */}
        <div className="space-y-6">
          {slide.fragments.map((fragment, index) => (
            <Fragment key={index} step={fragment.step} currentStep={stepIndex}>
              <div className="text-lg text-slate-800 dark:text-slate-200">
                {fragment.content}
              </div>
            </Fragment>
          ))}
        </div>
      </div>
    </div>
  );
}
