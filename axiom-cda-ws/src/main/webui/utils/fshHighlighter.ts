export const createFshLanguage = () => {
    const keywords = [
        'Profile', 'Parent', 'Id', 'Title', 'Description', 'Status', 'Kind', 'Abstract',
        'Extension', 'Invariant', 'Severity', 'Expression', 'XPath',
        'Instance', 'InstanceOf', 'Instanceof',
        'ValueSet', 'compose', 'exclude', 'include', 'system', 'concept',
        'CodeSystem', 'hierarchyMeaning', 'content', 'supplements',
        'Mapping', 'Source', 'Target', 'Id', 'Title', 'Description',
        'Logical', 'Resource', 'RuleSet', 'Alias',
        'only', 'contains', 'from', 'valueset', 'vs', 'cardinality', 'card',
        'short', 'definition', 'min', 'max', 'fixed', 'pattern', 'example',
        'mustSupport', 'isModifier', 'isSummary', 'defaultValue',
        'obeys', 'slices', 'discriminator', 'type', 'profile', 'targetProfile',
        'binding', 'strength', 'required', 'extensible', 'preferred', 'example',
        'code', 'system', 'version', 'display', 'filter', 'valueSet', 'value'
    ];

    const severityKeywords = ['error', 'warning'];

    const tokenize = (code: string) => {
        const lines = code.split('\n');
        const tokens = [];

        for (const line of lines) {
            if (line.trim() === '') {
                tokens.push({ type: 'whitespace', value: line });
                continue;
            }

            const trimmed = line.trim();
            const leadingWhitespace = line.match(/^\s*/)?.[0] || '';

            if (trimmed.startsWith('//')) {
                tokens.push({ type: 'comment', value: line });
                continue;
            }

            if (trimmed.startsWith('Profile:') ||
                trimmed.startsWith('Extension:') ||
                trimmed.startsWith('Invariant:') ||
                trimmed.startsWith('Instance:') ||
                trimmed.startsWith('ValueSet:') ||
                trimmed.startsWith('CodeSystem:') ||
                trimmed.startsWith('Mapping:') ||
                trimmed.startsWith('Logical:') ||
                trimmed.startsWith('Resource:') ||
                trimmed.startsWith('RuleSet:') ||
                trimmed.startsWith('Alias:')) {
                const match = trimmed.match(/^(Profile|Extension|Invariant|Instance|ValueSet|CodeSystem|Mapping|Logical|Resource|RuleSet|Alias):\s*(.*)$/);
                if (match) {
                    tokens.push({ type: 'keyword', value: leadingWhitespace + match[1] + ':' });
                    if (match[2]) {
                        tokens.push({ type: 'identifier', value: ' ' + match[2] });
                    }
                } else {
                    tokens.push({ type: 'text', value: line });
                }
                continue;
            }

            if (trimmed.startsWith('*')) {
                const starIndex = trimmed.indexOf('*');
                const beforeStar = leadingWhitespace;
                const afterStar = trimmed.substring(starIndex + 1).trim();

                tokens.push({ type: 'operator', value: beforeStar + '*' });

                let remaining = afterStar;

                while (remaining.length > 0) {
                    const stringMatch = remaining.match(/^"([^"]*)"/);
                    if (stringMatch) {
                        tokens.push({ type: 'string', value: '"' + stringMatch[1] + '"' });
                        remaining = remaining.substring(stringMatch[0].length).trimLeft();
                        continue;
                    }

                    const codeMatch = remaining.match(/^#(\S+)/);
                    if (codeMatch) {
                        tokens.push({ type: 'code', value: '#' + codeMatch[1] });
                        remaining = remaining.substring(codeMatch[0].length).trimLeft();
                        continue;
                    }

                    const booleanMatch = remaining.match(/^(true|false)/i);
                    if (booleanMatch) {
                        tokens.push({ type: 'boolean', value: booleanMatch[1] });
                        remaining = remaining.substring(booleanMatch[0].length).trimLeft();
                        continue;
                    }

                    const cardinalityMatch = remaining.match(/^(\d+|\*)\.\.(\d+|\*)/);
                    if (cardinalityMatch) {
                        tokens.push({ type: 'cardinality', value: cardinalityMatch[0] });
                        remaining = remaining.substring(cardinalityMatch[0].length).trimLeft();
                        continue;
                    }

                    if (remaining.startsWith('from')) {
                        tokens.push({ type: 'keyword', value: 'from' });
                        remaining = remaining.substring(4).trimLeft();
                        continue;
                    }

                    if (remaining.startsWith('only')) {
                        tokens.push({ type: 'keyword', value: 'only' });
                        remaining = remaining.substring(4).trimLeft();
                        continue;
                    }

                    if (remaining.startsWith('contains')) {
                        tokens.push({ type: 'keyword', value: 'contains' });
                        remaining = remaining.substring(8).trimLeft();
                        continue;
                    }

                    if (remaining.startsWith('(') && remaining.includes(')')) {
                        const parenEnd = remaining.indexOf(')');
                        const inside = remaining.substring(1, parenEnd).trim();

                        if (severityKeywords.includes(inside.toLowerCase())) {
                            tokens.push({ type: 'severity', value: '(' + inside + ')' });
                        } else {
                            tokens.push({ type: 'bracket', value: '(' + inside + ')' });
                        }
                        remaining = remaining.substring(parenEnd + 1).trimLeft();
                        continue;
                    }

                    const pathMatch = remaining.match(/^(\S+)/);
                    if (pathMatch) {
                        const path = pathMatch[1];
                        if (path.startsWith('^')) {
                            tokens.push({ type: 'meta', value: path });
                        } else if (keywords.includes(path)) {
                            tokens.push({ type: 'keyword', value: path });
                        } else {
                            tokens.push({ type: 'property', value: path });
                        }
                        remaining = remaining.substring(path.length).trimLeft();
                        continue;
                    }

                    if (remaining.startsWith('=')) {
                        tokens.push({ type: 'operator', value: '=' });
                        remaining = remaining.substring(1).trimLeft();
                        continue;
                    }

                    if (remaining.length > 0) {
                        tokens.push({ type: 'text', value: remaining[0] });
                        remaining = remaining.substring(1);
                    }
                }
                continue;
            }

            tokens.push({ type: 'text', value: line });
        }

        return tokens;
    };

    return { tokenize };
};
