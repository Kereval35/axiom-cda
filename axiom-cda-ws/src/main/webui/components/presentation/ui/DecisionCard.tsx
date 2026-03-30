import { ReactNode } from 'react';

interface DecisionCardProps {
  title: string;
  children: ReactNode;
}

export function DecisionCard({ title, children }: DecisionCardProps) {
  return (
    <div className="bg-gradient-to-br from-purple-50 via-purple-50 to-indigo-100 dark:from-purple-900/40 dark:via-purple-800/40 dark:to-indigo-900/40 border-2 border-purple-300 dark:border-purple-600 rounded-xl p-6 shadow-lg">
      <div className="flex items-start gap-3 mb-3">
        <span className="text-2xl">🎯</span>
        <h3 className="text-lg font-semibold text-purple-950 dark:text-purple-100">{title}</h3>
      </div>
      <div className="text-purple-900 dark:text-purple-200 leading-relaxed pl-11">{children}</div>
    </div>
  );
}
