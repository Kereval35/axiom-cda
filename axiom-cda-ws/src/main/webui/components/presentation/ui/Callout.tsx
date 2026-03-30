import { ReactNode } from 'react';

interface CalloutProps {
  variant?: 'info' | 'warning' | 'decision' | 'success';
  children: ReactNode;
}

export function Callout({ variant = 'info', children }: CalloutProps) {
  const variants = {
    info: 'bg-gradient-to-r from-blue-50 to-blue-100/50 dark:from-blue-900/40 dark:to-blue-800/40 border-blue-300 dark:border-blue-600 text-blue-950 dark:text-blue-100 shadow-md',
    warning: 'bg-gradient-to-r from-amber-50 to-amber-100/50 dark:from-amber-900/40 dark:to-amber-800/40 border-amber-300 dark:border-amber-600 text-amber-950 dark:text-amber-100 shadow-md',
    decision: 'bg-gradient-to-r from-purple-50 to-purple-100/50 dark:from-purple-900/40 dark:to-purple-800/40 border-purple-300 dark:border-purple-600 text-purple-950 dark:text-purple-100 shadow-md',
    success: 'bg-gradient-to-r from-green-50 to-green-100/50 dark:from-green-900/40 dark:to-green-800/40 border-green-300 dark:border-green-600 text-green-950 dark:text-green-100 shadow-md',
  };

  const icons = {
    info: '💡',
    warning: '⚠️',
    decision: '🎯',
    success: '✓',
  };

  return (
    <div className={`border-l-4 p-5 rounded-r-xl ${variants[variant]}`}>
      <div className="flex items-start gap-3">
        <span className="text-2xl flex-shrink-0">{icons[variant]}</span>
        <div className="flex-1 text-base leading-relaxed">{children}</div>
      </div>
    </div>
  );
}
