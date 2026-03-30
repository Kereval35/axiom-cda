interface LineInfo {
    original: string;
    trimmed: string;
    indentLevel: number;
    type: 'header' | 'rule' | 'comment' | 'empty';
}

export const formatFsh = (fsh: string): string => {
    const lines = fsh.split('\n');
    const lineInfos: LineInfo[] = [];
    let blankLineCount = 0;

    const isSectionHeader = (trimmed: string): boolean => {
        return trimmed.startsWith('Profile:') ||
               trimmed.startsWith('Extension:') ||
               trimmed.startsWith('Invariant:') ||
               trimmed.startsWith('Instance:') ||
               trimmed.startsWith('ValueSet:') ||
               trimmed.startsWith('CodeSystem:') ||
               trimmed.startsWith('Mapping:') ||
               trimmed.startsWith('Logical:') ||
               trimmed.startsWith('Resource:') ||
               trimmed.startsWith('RuleSet:') ||
               trimmed.startsWith('Alias:');
    };

    const getIndentationLevel = (trimmed: string): number => {
        if (!trimmed || trimmed.startsWith('//')) return 0;
        if (isSectionHeader(trimmed)) return 0;
        if (trimmed.startsWith('*')) {
            const starIndex = trimmed.indexOf('*');
            const contentAfterStar = trimmed.substring(starIndex + 1).trim();
            if (contentAfterStar.startsWith('*')) {
                return 2;
            }
            return 1;
        }
        return 0;
    };

    for (const line of lines) {
        const trimmed = line.trim();

        if (!trimmed) {
            blankLineCount++;
            if (blankLineCount <= 1) {
                lineInfos.push({
                    original: line,
                    trimmed,
                    indentLevel: 0,
                    type: 'empty'
                });
            }
            continue;
        }

        blankLineCount = 0;

        let type: LineInfo['type'] = 'rule';
        if (isSectionHeader(trimmed)) {
            type = 'header';
        } else if (trimmed.startsWith('//')) {
            type = 'comment';
        }

        const indentLevel = getIndentationLevel(trimmed);

        lineInfos.push({
            original: line,
            trimmed,
            indentLevel,
            type
        });
    }

    const formatted: string[] = [];

    for (let i = 0; i < lineInfos.length; i++) {
        const info = lineInfos[i];

        if (info.type === 'empty') {
            formatted.push('');
            continue;
        }

        if (info.type === 'header') {
            if (formatted.length > 0 && formatted[formatted.length - 1] !== '') {
                formatted.push('');
            }
            formatted.push(info.trimmed);
            formatted.push('');
            continue;
        }

        const indent = '  '.repeat(info.indentLevel);

        if (info.type === 'comment') {
            formatted.push(indent + info.trimmed);
            continue;
        }

        if (info.type === 'rule') {
            if (!info.trimmed.startsWith('*')) {
                formatted.push(indent + info.trimmed);
                continue;
            }

            let ruleContent = info.trimmed.substring(1).trim();

            if (ruleContent.includes(' = ')) {
                const parts = ruleContent.split(' = ');
                const key = parts[0];
                const value = parts.slice(1).join(' = ');
                ruleContent = `${key} = ${value}`;
            }

            formatted.push(indent + '* ' + ruleContent);
        }
    }

    while (formatted.length > 0 && formatted[formatted.length - 1] === '') {
        formatted.pop();
    }

    return formatted.join('\n');
};
