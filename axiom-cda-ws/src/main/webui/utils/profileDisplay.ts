"use client";

import { FshProfile } from "../types/generation";

export function sortProfilesForDisplay(profiles: FshProfile[]): FshProfile[] {
  return [...profiles].sort((left, right) => {
    if (left.fhirTransformEligible !== right.fhirTransformEligible) {
      return left.fhirTransformEligible ? -1 : 1;
    }
    return getReadableProfileName(left.name).localeCompare(getReadableProfileName(right.name));
  });
}

export function getReadableProfileName(name: string): string {
  if (!name) {
    return "";
  }

  const displayName = name.replace(/Fhir[A-Z][A-Za-z0-9]*$/, "").trim() || name;

  const spaced = displayName
    .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
    .replace(/([A-Z]+)([A-Z][a-z])/g, "$1 $2")
    .replace(/([A-Za-z])([0-9])/g, "$1 $2")
    .replace(/([0-9])([A-Za-z])/g, "$1 $2")
    .replace(/_/g, " ")
    .trim();

  return spaced.replace(/\s+/g, " ");
}
