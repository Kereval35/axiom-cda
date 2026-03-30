import React from "react";

interface Token {
    type: string;
    value: string;
}

interface FshHighlighterProps {
    code: string;
    className?: string;
}

export const FshHighlighter: React.FC<FshHighlighterProps> = ({ code, className = "" }) => {
    const tokenColors: { [key: string]: string } = {
        keyword: "text-indigo-600 dark:text-indigo-400 font-semibold",
        meta: "text-fuchsia-600 dark:text-fuchsia-400 font-semibold",
        property: "text-cyan-700 dark:text-cyan-400 font-medium",
        operator: "text-pink-600 dark:text-pink-400",
        string: "text-green-700 dark:text-green-400",
        code: "text-amber-600 dark:text-amber-400",
        boolean: "text-violet-600 dark:text-violet-400",
        cardinality: "text-orange-600 dark:text-orange-400 font-mono",
        severity: "text-rose-600 dark:text-rose-400 font-semibold",
        bracket: "text-zinc-600 dark:text-zinc-400",
        comment: "text-zinc-500 dark:text-zinc-500 italic",
        text: "text-zinc-800 dark:text-zinc-300",
        whitespace: "text-zinc-800 dark:text-zinc-300",
        identifier: "text-zinc-800 dark:text-zinc-300 font-medium",
        url: "text-sky-600 dark:text-sky-400 underline decoration-sky-300/70",
        target: "text-emerald-700 dark:text-emerald-400 font-semibold",
    };

    const urlRegex = /(https?:\/\/\S+|urn:oid:\S+)/g;

    const pushTargetValueTokens = (value: string, tokens: Token[]) => {
        let lastIndex = 0;
        let match: RegExpExecArray | null;
        while ((match = urlRegex.exec(value)) !== null) {
            const before = value.slice(lastIndex, match.index);
            if (before) {
                const parts = before.split(/(\s+)/);
                parts.forEach((part) => {
                    if (!part) return;
                    tokens.push({
                        type: part.trim() ? 'target' : 'whitespace',
                        value: part
                    });
                });
            }
            tokens.push({ type: 'url', value: match[0] });
            lastIndex = match.index + match[0].length;
        }
        const tail = value.slice(lastIndex);
        if (tail) {
            const parts = tail.split(/(\s+)/);
            parts.forEach((part) => {
                if (!part) return;
                tokens.push({
                    type: part.trim() ? 'target' : 'whitespace',
                    value: part
                });
            });
        }
    };

    const tokenizeLine = (line: string): Token[] => {
        const trimmed = line.trim();
        const leadingWhitespace = line.match(/^\s*/)?.[0] || '';
        const tokens: Token[] = [];

        // Empty line
        if (trimmed === '') {
            return [{ type: 'whitespace', value: line }];
        }

        // Comment line
        if (trimmed.startsWith('//')) {
            return [{ type: 'comment', value: line }];
        }

        // Header line (Profile:, Extension:, etc.)
        const headerMatch = trimmed.match(/^(Profile|Extension|Invariant|Instance|ValueSet|CodeSystem|Mapping|Logical|Resource|RuleSet|Alias):\s*(.*)$/);
        if (headerMatch) {
            tokens.push({ type: 'whitespace', value: leadingWhitespace });
            tokens.push({ type: 'keyword', value: headerMatch[1] + ':' });
            if (headerMatch[2]) {
                tokens.push({ type: 'identifier', value: ' ' + headerMatch[2] });
            }
            return tokens;
        }

        // Metadata lines like Parent: / InstanceOf:
        const targetLineMatch = trimmed.match(/^(Parent|InstanceOf|Instanceof):\s*(.*)$/);
        if (targetLineMatch) {
            tokens.push({ type: 'whitespace', value: leadingWhitespace });
            tokens.push({ type: 'keyword', value: targetLineMatch[1] + ':' });
            if (targetLineMatch[2]) {
                tokens.push({ type: 'whitespace', value: ' ' });
                pushTargetValueTokens(targetLineMatch[2], tokens);
            }
            return tokens;
        }

        // Rule line (starts with *)
        if (trimmed.startsWith('*')) {
            tokens.push({ type: 'whitespace', value: leadingWhitespace });
            tokens.push({ type: 'operator', value: '*' });

            let remaining = line.substring(line.indexOf('*') + 1);

            while (remaining.length > 0) {
                const whitespaceMatch = remaining.match(/^\s+/);
                if (whitespaceMatch) {
                    tokens.push({ type: 'whitespace', value: whitespaceMatch[0] });
                    remaining = remaining.substring(whitespaceMatch[0].length);
                    continue;
                }

                const targetMatch = remaining.match(/^(Parent|InstanceOf|Instanceof):\s*(.*)$/);
                if (targetMatch) {
                    tokens.push({ type: 'keyword', value: targetMatch[1] + ':' });
                    if (targetMatch[2]) {
                        tokens.push({ type: 'whitespace', value: ' ' });
                        pushTargetValueTokens(targetMatch[2], tokens);
                    }
                    remaining = '';
                    continue;
                }

                // String literal (preserve spaces inside quotes)
                const stringMatch = remaining.match(/^"([^"]*)"/);
                if (stringMatch) {
                    tokens.push({ type: 'string', value: stringMatch[0] });
                    remaining = remaining.substring(stringMatch[0].length);
                    continue;
                }

                // Severity in parentheses (error, warning)
                const severityMatch = remaining.match(/^\((error|warning)\)/i);
                if (severityMatch) {
                    tokens.push({ type: 'severity', value: severityMatch[0] });
                    remaining = remaining.substring(severityMatch[0].length);
                    continue;
                }

                // Cardinality (e.g., 0..1, 1..*)
                const cardinalityMatch = remaining.match(/^(\d+|\*)\.\.(\d+|\*)/);
                if (cardinalityMatch) {
                    tokens.push({ type: 'cardinality', value: cardinalityMatch[0] });
                    remaining = remaining.substring(cardinalityMatch[0].length);
                    continue;
                }

                // URL or URN
                const urlMatch = remaining.match(/^(https?:\/\/\S+|urn:oid:\S+)/);
                if (urlMatch) {
                    tokens.push({ type: 'url', value: urlMatch[0] });
                    remaining = remaining.substring(urlMatch[0].length);
                    continue;
                }

                // Code (e.g., #code)
                const codeMatch = remaining.match(/^#(\S+)/);
                if (codeMatch) {
                    tokens.push({ type: 'code', value: codeMatch[0] });
                    remaining = remaining.substring(codeMatch[0].length);
                    continue;
                }

                // Meta property (starts with ^)
                const metaMatch = remaining.match(/^(\^[a-zA-Z0-9\.]+)/);
                if (metaMatch) {
                    tokens.push({ type: 'meta', value: metaMatch[1] });
                    remaining = remaining.substring(metaMatch[1].length);
                    continue;
                }

                // Keywords
                const keywordMatch = remaining.match(/^(from|only|contains|obeys|and|or)\b/);
                if (keywordMatch) {
                    tokens.push({ type: 'keyword', value: keywordMatch[1] });
                    remaining = remaining.substring(keywordMatch[1].length);
                    continue;
                }

                // Boolean
                const booleanMatch = remaining.match(/^(true|false)\b/i);
                if (booleanMatch) {
                    tokens.push({ type: 'boolean', value: booleanMatch[1] });
                    remaining = remaining.substring(booleanMatch[1].length);
                    continue;
                }

                // Operator
                const operatorMatch = remaining.match(/^(=|:=|<=|>=|<|>)/);
                if (operatorMatch) {
                    tokens.push({ type: 'operator', value: operatorMatch[1] });
                    remaining = remaining.substring(operatorMatch[1].length);
                    continue;
                }

                // Identifier/property-like chunk
                const chunkMatch = remaining.match(/^([a-zA-Z0-9_\.\[\]-]+)/);
                if (chunkMatch) {
                    tokens.push({ type: 'property', value: chunkMatch[1] });
                    remaining = remaining.substring(chunkMatch[1].length);
                    continue;
                }

                // Fallback: take the next character
                tokens.push({ type: 'text', value: remaining[0] });
                remaining = remaining.substring(1);
            }

            return tokens;
        }

        // Default: return as text
        return [{ type: 'text', value: line }];
    };

    const lines = code.split('\n');

    const lineElements = lines.map((line, lineIndex) => {
        const lineTokens = tokenizeLine(line);

        return (
            <div key={`line-${lineIndex}`} className="flex">
                <span className="inline-block w-12 text-right pr-4 text-zinc-500 dark:text-zinc-600 font-mono text-sm shrink-0 select-none">
                    {lineIndex + 1}
                </span>
                <span className="flex-1 font-mono text-sm whitespace-pre">
                    {lineTokens.length === 0 ? (
                        '\u00A0'
                    ) : (
                        lineTokens.map((token, tokenIndex) => {
                            const colorClass = tokenColors[token.type] || tokenColors.text;
                            return (
                                <span key={`token-${tokenIndex}`} className={colorClass}>
                                    {token.value}
                                </span>
                            );
                        })
                    )}
                </span>
            </div>
        );
    });

    return (
        <div className={`bg-zinc-50 dark:bg-zinc-950 rounded-lg overflow-x-auto p-4 ${className}`}>
            {lineElements}
        </div>
    );
};
