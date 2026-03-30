'use client';

import { useState, useEffect, useCallback } from 'react';
import type { Slide } from '@/types/presentation/types';

export function useDeckNavigation(slides: Slide[]) {
  const [slideIndex, setSlideIndex] = useState(0);
  const [stepIndex, setStepIndex] = useState(0);

  const currentSlide = slides[slideIndex];
  const maxSteps = currentSlide?.stepsCount || 0;

  const next = useCallback(() => {
    if (stepIndex < maxSteps) {
      // Reveal next fragment
      setStepIndex(stepIndex + 1);
    } else if (slideIndex < slides.length - 1) {
      // Move to next slide
      setSlideIndex(slideIndex + 1);
      setStepIndex(0);
    }
  }, [slideIndex, stepIndex, slides.length, maxSteps]);

  const prev = useCallback(() => {
    if (stepIndex > 0) {
      // Hide current fragment
      setStepIndex(stepIndex - 1);
    } else if (slideIndex > 0) {
      // Move to previous slide
      const prevSlideIndex = slideIndex - 1;
      setSlideIndex(prevSlideIndex);
      setStepIndex(slides[prevSlideIndex]?.stepsCount || 0);
    }
  }, [slideIndex, stepIndex, slides]);

  const goToSlide = useCallback((index: number) => {
    if (index >= 0 && index < slides.length) {
      setSlideIndex(index);
      setStepIndex(0);
    }
  }, [slides.length]);

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'ArrowRight' || e.key === ' ') {
        e.preventDefault();
        next();
      } else if (e.key === 'ArrowLeft') {
        e.preventDefault();
        prev();
      } else if (e.key === 'f' || e.key === 'F') {
        // Toggle fullscreen
        if (!document.fullscreenElement) {
          document.documentElement.requestFullscreen();
        } else {
          document.exitFullscreen();
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [next, prev]);

  return {
    slideIndex,
    stepIndex,
    currentSlide,
    totalSlides: slides.length,
    next,
    prev,
    goToSlide,
  };
}
