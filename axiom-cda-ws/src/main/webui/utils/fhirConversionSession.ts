"use client";

import { FshProfile, IRTemplate } from "../types/generation";

const STORAGE_KEY = "axiom-cda-fhir-conversion";

export interface FhirConversionSession {
    profiles: FshProfile[];
    irTemplates: IRTemplate[];
    selectedProfileName: string;
    selectedTemplateId: string;
}

export function saveFhirConversionSession(session: FhirConversionSession): void {
    if (typeof window === "undefined") {
        return;
    }
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function loadFhirConversionSession(): FhirConversionSession | null {
    if (typeof window === "undefined") {
        return null;
    }
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) {
        return null;
    }
    try {
        return JSON.parse(raw) as FhirConversionSession;
    } catch {
        return null;
    }
}
