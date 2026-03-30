import { ReactNode } from 'react';

interface CodeBlockProps {
  language?: string;
  children: ReactNode;
  highlight?: boolean;
}

export function CodeBlock({ language = 'text', children, highlight = false }: CodeBlockProps) {
  return (
    <div className={`relative rounded-xl overflow-hidden shadow-lg ${highlight ? 'ring-2 ring-blue-500' : ''}`}>
      {language && (
        <div className="bg-gradient-to-r from-slate-800 to-slate-700 text-slate-200 text-xs px-4 py-2 font-mono font-semibold border-b border-slate-600">
          {language}
        </div>
      )}
      <pre className="bg-gradient-to-br from-slate-900 to-slate-950 text-slate-100 p-5 overflow-x-auto">
        <code className="font-mono text-sm leading-relaxed">{children}</code>
      </pre>
    </div>
  );
}
