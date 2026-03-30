import { useState } from "react";
import { GenerationOptions, GenerationResult } from "../types/generation";
import { generateFshAction } from "../actions/generation";

export function useGeneration() {
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState<GenerationResult | null>(null);
    const [error, setError] = useState<string | null>(null);

    const generate = async (options: GenerationOptions) => {
        setLoading(true);
        setError(null);
        setResult(null);

        try {
            const data = await generateFshAction(options);
            setResult(data);
        } catch (err) {
            setError(err instanceof Error ? err.message : "An unknown error occurred");
        } finally {
            setLoading(false);
        }
    };

    const clearResult = () => {
        setResult(null);
        setError(null);
    };

    return {
        loading,
        result,
        error,
        generate,
        clearResult,
    };
}
