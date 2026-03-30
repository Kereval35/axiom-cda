import { Badge } from './Badge';

export function FlowDiagram() {
  return (
    <div className="flex items-center justify-center gap-6 py-8">
      <div className="flex flex-col items-center">
        <div className="bg-gradient-to-br from-orange-100 to-orange-200 dark:from-orange-800/60 dark:to-orange-700/60 border-2 border-orange-400 dark:border-orange-600 rounded-xl p-6 min-w-[140px] text-center shadow-lg transform hover:scale-105 transition-transform">
          <Badge variant="bbr">BBR</Badge>
          <div className="text-xs text-orange-800 dark:text-orange-200 mt-2 font-semibold">ART-DECOR</div>
        </div>
        <div className="text-xs text-slate-600 dark:text-slate-400 mt-2 font-medium">Export XML</div>
      </div>

      <div className="flex flex-col items-center">
        <svg className="w-12 h-12 text-blue-600 dark:text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M13 7l5 5m0 0l-5 5m5-5H6" />
        </svg>
      </div>

      <div className="flex flex-col items-center">
        <div className="bg-gradient-to-br from-blue-100 to-blue-200 dark:from-blue-800/60 dark:to-blue-700/60 border-2 border-blue-400 dark:border-blue-600 rounded-xl p-6 min-w-[140px] text-center shadow-lg transform hover:scale-105 transition-transform">
          <Badge variant="ir">IR</Badge>
          <div className="text-xs text-blue-800 dark:text-blue-200 mt-2 font-semibold">Intermediate</div>
        </div>
        <div className="text-xs text-slate-600 dark:text-slate-400 mt-2 font-medium">JSON stable</div>
      </div>

      <div className="flex flex-col items-center">
        <svg className="w-12 h-12 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M13 7l5 5m0 0l-5 5m5-5H6" />
        </svg>
      </div>

      <div className="flex flex-col items-center">
        <div className="bg-gradient-to-br from-green-100 to-green-200 dark:from-green-800/60 dark:to-green-700/60 border-2 border-green-400 dark:border-green-600 rounded-xl p-6 min-w-[140px] text-center shadow-lg transform hover:scale-105 transition-transform">
          <Badge variant="fsh">FSH</Badge>
          <div className="text-xs text-green-800 dark:text-green-200 mt-2 font-semibold">Profiles CDA</div>
        </div>
        <div className="text-xs text-slate-600 dark:text-slate-400 mt-2 font-medium">SUSHI-ready</div>
      </div>
    </div>
  );
}
