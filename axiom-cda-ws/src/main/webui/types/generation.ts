export interface GenerationReport {
    templatesConsidered: number;
    templatesGenerated: number;
    templatesSkipped: number;
    templatesOk: number;
    profilesGenerated: number;
    invariantsGenerated: number;
    unmappedElements: number;
    unresolvedValueSets: number;
    warnings: string[];
    errors: string[];
}

export interface FshProfile {
    name: string;
    content: string;
}

export interface IRTemplate {
    id: string;
    name: string;
    displayName: string;
    description: string;
    rootCdaType: string;
    elements: any[];
    includes: any[];
    invariants: any[];
}

export interface GenerationResult {
    zipBase64: string;
    report: GenerationReport;
    profiles: FshProfile[];
    irTemplates: IRTemplate[];
}

export interface GenerationOptions {
    bbr: string;
    sushiRepo: boolean;
    emitIr: boolean;
    emitLogs: boolean;
    yamlConfig: string | null;
}
