export interface IRTemplateElement {
    name?: string;
    path: string;
    cardinality: unknown;
    datatype: string | null;
    fixedValue: string | null;
    fixedValueType: string | null;
    bindings: unknown[];
    shortDescription: string | null;
}

export interface IRTemplateInclude {
    id?: string;
    path: string;
    templateId: string;
    cardinality: unknown;
}

export interface IRTemplateInvariant {
    id?: string;
    name: string;
    description: string | null;
    severity: string;
    expression: string;
}

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
    templateId: string | null;
    rootCdaType: string | null;
    fhirTransformEligible: boolean;
    fhirTransformKind: string | null;
    fhirTransformNotice: string | null;
}

export interface IRTemplate {
    id: string;
    name: string;
    displayName: string;
    description: string;
    rootCdaType: string;
    elements: IRTemplateElement[];
    includes: IRTemplateInclude[];
    invariants: IRTemplateInvariant[];
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

export interface FhirConversionResult {
    profiles: FshProfile[];
    diagnostics: string[];
}

export interface SushiCompileResult {
    success: boolean;
    structureDefinitionJson: string | null;
    diagnostics: string[];
    sushiConfig: string | null;
    generatedFileName: string | null;
}

export interface FhirPackagePreset {
    label: string;
    packageId: string;
    version: string;
    description: string | null;
}
