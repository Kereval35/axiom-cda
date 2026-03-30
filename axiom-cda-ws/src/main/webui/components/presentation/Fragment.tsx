'use client';

import { motion, AnimatePresence } from 'framer-motion';
import { ReactNode, useRef, useEffect } from 'react';

interface FragmentProps {
  step: number;
  currentStep: number;
  children: ReactNode;
  direction?: 'up' | 'down' | 'left' | 'right';
}

export function Fragment({ step, currentStep, children, direction = 'up' }: FragmentProps) {
  const isVisible = currentStep >= step;
  const ref = useRef<HTMLDivElement>(null);

  const directionOffset = {
    up: { y: 16 },
    down: { y: -16 },
    left: { x: 16 },
    right: { x: -16 },
  };

  useEffect(() => {
    if (isVisible && ref.current) {
      // Wait for the animation to start, then scroll to max bottom
      setTimeout(() => {
        const scrollContainer = ref.current?.closest('.overflow-y-auto');
        if (scrollContainer) {
          scrollContainer.scrollTo({
            top: scrollContainer.scrollHeight,
            behavior: 'smooth'
          });
        }
      }, 100);
    }
  }, [isVisible]);

  return (
    <AnimatePresence mode="wait">
      {isVisible && (
        <motion.div
          ref={ref}
          initial={{ opacity: 0, ...directionOffset[direction] }}
          animate={{ opacity: 1, y: 0, x: 0 }}
          exit={{ opacity: 0, ...directionOffset[direction] }}
          transition={{
            duration: 0.3,
            ease: [0.22, 1, 0.36, 1], // Custom easing for smooth feel
          }}
        >
          {children}
        </motion.div>
      )}
    </AnimatePresence>
  );
}
