import React from "react";

interface JsonHighlighterProps {
    json: string;
}

interface Token {
    type: "key" | "string" | "number" | "boolean" | "null" | "punctuation" | "whitespace";
    value: string;
}

export const JsonHighlighter: React.FC<JsonHighlighterProps> = ({ json }) => {
    const formatted = React.useMemo(() => {
        try {
            return JSON.stringify(JSON.parse(json), null, 2);
        } catch {
            return json;
        }
    }, [json]);

    const tokens = React.useMemo(() => tokenizeJson(formatted), [formatted]);
    const colors: Record<Token["type"], string> = {
        key: "text-cyan-700 dark:text-cyan-400",
        string: "text-emerald-700 dark:text-emerald-400",
        number: "text-amber-700 dark:text-amber-400",
        boolean: "text-violet-700 dark:text-violet-400",
        null: "text-rose-700 dark:text-rose-400",
        punctuation: "text-zinc-500 dark:text-zinc-400",
        whitespace: "text-zinc-800 dark:text-zinc-200",
    };

    return (
        <pre className="max-h-96 overflow-auto rounded-lg bg-white p-4 font-mono text-xs leading-relaxed dark:bg-zinc-900">
            {tokens.map((token, index) => (
                <span key={`${token.value}-${index}`} className={colors[token.type]}>
                    {token.value}
                </span>
            ))}
        </pre>
    );
};

function tokenizeJson(value: string): Token[] {
    const tokens: Token[] = [];
    const pattern = /("(?:\\.|[^"\\])*"(?=\s*:))|("(?:\\.|[^"\\])*")|(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)|\b(true|false)\b|\bnull\b|([{}\[\],:])|(\s+)/g;
    let lastIndex = 0;
    let match: RegExpExecArray | null;
    while ((match = pattern.exec(value)) !== null) {
        if (match.index > lastIndex) {
            tokens.push({ type: "string", value: value.slice(lastIndex, match.index) });
        }
        if (match[1]) {
            tokens.push({ type: "key", value: match[1] });
        } else if (match[2]) {
            tokens.push({ type: "string", value: match[2] });
        } else if (match[3]) {
            tokens.push({ type: "number", value: match[3] });
        } else if (match[4]) {
            tokens.push({ type: "boolean", value: match[4] });
        } else if (match[0] === "null") {
            tokens.push({ type: "null", value: match[0] });
        } else if (match[5]) {
            tokens.push({ type: "punctuation", value: match[5] });
        } else if (match[6]) {
            tokens.push({ type: "whitespace", value: match[6] });
        }
        lastIndex = pattern.lastIndex;
    }
    if (lastIndex < value.length) {
        tokens.push({ type: "string", value: value.slice(lastIndex) });
    }
    return tokens;
}
