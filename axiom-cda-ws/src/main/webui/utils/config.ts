/**
 * Application configuration
 * This file centralizes the basePath configuration to avoid hardcoding it everywhere
 */

export const BASE_PATH = "/axiom-cda";
export const API_BASE_PATH = "/api";

/**
 * Helper function to prepend the base path to any path
 * @param path - The path to prepend the base path to
 * @returns The full path with base path
 */
export function withBasePath(path: string): string {
  // Remove leading slash if present to avoid double slashes
  const cleanPath = path.startsWith("/") ? path.slice(1) : path;
  return `${BASE_PATH}/${cleanPath}`;
}

/**
 * Helper function for backend API routes, which are not mounted under the UI base path.
 * @param path - The API path relative to /api
 * @returns The full API path
 */
export function withApiPath(path: string): string {
  const cleanPath = path.startsWith("/") ? path.slice(1) : path;
  if (cleanPath.startsWith("api/")) {
    return `/${cleanPath}`;
  }
  return `${API_BASE_PATH}/${cleanPath}`;
}

/**
 * Helper function for public assets
 * @param assetPath - The path to the asset in the public folder
 * @returns The full path with base path
 */
export function publicAsset(assetPath: string): string {
  return withBasePath(assetPath);
}
