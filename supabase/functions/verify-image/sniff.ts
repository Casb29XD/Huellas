// Tipo real de la imagen por sus primeros bytes; null si no es JPEG, PNG ni WebP.
const PNG = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];
const ascii = (b: Uint8Array, from: number, to: number) => String.fromCharCode(...b.subarray(from, to));

export function sniffImage(b: Uint8Array): "image/jpeg" | "image/png" | "image/webp" | null {
  if (b.length >= 3 && b[0] === 0xff && b[1] === 0xd8 && b[2] === 0xff) return "image/jpeg";
  if (b.length >= 8 && PNG.every((v, i) => b[i] === v)) return "image/png";
  if (b.length >= 12 && ascii(b, 0, 4) === "RIFF" && ascii(b, 8, 12) === "WEBP") return "image/webp";
  return null;
}

/** Ruta de objeto segura dentro del bucket: <carpeta>/<archivo>, sin "..", con cada segmento codificado. */
export function objectPath(name: unknown): string | null {
  if (typeof name !== "string" || !name || name.length > 512) return null;
  const parts = name.split("/");
  if (parts.some((p) => !p || p === "." || p === "..")) return null;
  return parts.map(encodeURIComponent).join("/");
}

/**
 * ¿El Bearer es un JWT de service_role? La firma ya la validó el gateway (verify_jwt = true); aquí solo se
 * lee el claim role, que no depende de cuál de las claves service_role del proyecto se usó.
 */
export function isServiceRole(authorization: string | null): boolean {
  const part = authorization?.match(/^Bearer\s+[^.]+\.([^.]+)\.[^.]+$/)?.[1];
  if (!part) return false;
  try {
    const b64 = part.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(part.length / 4) * 4, "=");
    return JSON.parse(atob(b64)).role === "service_role";
  } catch {
    return false;
  }
}
