import { GenerationOptions, GenerationResult } from "../types/generation";
import { withBasePath } from "@/utils/config";

export async function generateFshAction(options: GenerationOptions): Promise<GenerationResult> {
    const response = await fetch(withBasePath("api/generate"), {
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
        }),
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "Generation failed on backend");
    }

    return await response.json();
}
