import { FhirBuiltInMappingPreset, FhirConversionResult, FhirPackagePreset, GenerationOptions, GenerationResult, IRTemplate, SushiCompileResult } from "../types/generation";
import { withApiPath } from "@/utils/config";

export async function generateFshAction(options: GenerationOptions): Promise<GenerationResult> {
    const response = await fetch(withApiPath("generate"), {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            bbr: options.bbr,
            sushiRepo: options.sushiRepo,
            emitIr: options.emitIr,
            emitLogs: options.emitLogs,
            yamlConfig: options.yamlConfig,
            projectPlusRequiredIncludes: options.projectPlusRequiredIncludes,
            ownedRepositoryPrefixes: options.ownedRepositoryPrefixes,
        }),
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "Generation failed on backend");
    }

    return await response.json();
}

export async function convertFhirAction(options: {
    sourceProfileName: string;
    template: IRTemplate;
    structureMap: string | null;
    builtInMappingId: string | null;
}): Promise<FhirConversionResult> {
    const response = await fetch(withApiPath("convert/fhir"), {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(options),
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "FHIR conversion failed on backend");
    }

    return await response.json();
}

export async function compileFhirSushiAction(options: {
    profileName: string;
    fshContent: string;
    parent: string;
    dependencyPackageId: string | null;
    dependencyVersion: string | null;
}): Promise<SushiCompileResult> {
    const response = await fetch(withApiPath("convert/fhir/sushi"), {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(options),
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "SUSHI compilation failed on backend");
    }

    return await response.json();
}

export async function getFhirPackagePresetsAction(): Promise<FhirPackagePreset[]> {
    const response = await fetch(withApiPath("convert/fhir/package-presets"));

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "Unable to load FHIR package presets");
    }

    return await response.json();
}

export async function getFhirBuiltInMappingPresetsAction(): Promise<FhirBuiltInMappingPreset[]> {
    const response = await fetch(withApiPath("convert/fhir/mapping-presets"));

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "Unable to load built-in mapping presets");
    }

    return await response.json();
}
