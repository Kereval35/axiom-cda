import { ReactNode } from 'react';

interface RuleCardProps {
  condition: string;
  result: string | ReactNode;
}

export function RuleCard({ condition, result }: RuleCardProps) {
  return (
    <div className="bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-800/60 dark:to-slate-700/60 border-2 border-slate-300 dark:border-slate-600 rounded-lg p-5 shadow-md">
      <div className="space-y-3">
        <div>
          <span className="inline-block text-xs font-bold text-white bg-slate-600 dark:bg-slate-700 px-2 py-1 rounded uppercase tracking-wide">
            Si
          </span>
          <p className="text-slate-900 dark:text-slate-100 font-medium mt-2">{condition}</p>
        </div>
        <div className="border-t-2 border-slate-300 dark:border-slate-600 pt-3">
          <span className="inline-block text-xs font-bold text-white bg-blue-600 dark:bg-blue-700 px-2 py-1 rounded uppercase tracking-wide">
            Alors
          </span>
          <div className="text-slate-900 dark:text-slate-100 mt-2">{result}</div>
        </div>
      </div>
    </div>
  );
}
