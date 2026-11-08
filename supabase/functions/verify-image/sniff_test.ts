// deno test supabase/functions/verify-image/
import { assertEquals } from "jsr:@std/assert@1";
import { isServiceRole, objectPath, sniffImage } from "./sniff.ts";

const bytes = (...v: number[]) => new Uint8Array(v);
const text = (s: string) => new TextEncoder().encode(s);

Deno.test("reconoce JPEG, PNG y WebP por sus magic bytes", () => {
  assertEquals(sniffImage(bytes(0xff, 0xd8, 0xff, 0xd9)), "image/jpeg");
  assertEquals(sniffImage(bytes(0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0)), "image/png");
  assertEquals(sniffImage(text("RIFF\0\0\0\0WEBPVP8 ")), "image/webp");
});

Deno.test("HTML o texto declarados como image/jpeg no pasan", () => {
  assertEquals(sniffImage(text("<html><script>alert(1)</script>")), null);
  assertEquals(sniffImage(text("jpg")), null);
  assertEquals(sniffImage(bytes()), null);
  assertEquals(sniffImage(text("RIFF\0\0\0\0WAVEfmt ")), null);
});

Deno.test("la ruta del objeto no sale del bucket", () => {
  assertEquals(objectPath("u1/foto 1.jpg"), "u1/foto%201.jpg");
  assertEquals(objectPath("u1/../u2/x.jpg"), null);
  assertEquals(objectPath("/x.jpg"), null);
  assertEquals(objectPath(42), null);
});

Deno.test("solo un JWT con role service_role pasa", () => {
  const jwt = (claims: unknown) => `Bearer h.${btoa(JSON.stringify(claims)).replace(/=+$/, "")}.firma`;
  assertEquals(isServiceRole(jwt({ role: "service_role" })), true);
  assertEquals(isServiceRole(jwt({ role: "anon" })), false);
  assertEquals(isServiceRole(jwt({ role: "authenticated", sub: "u1" })), false);
  assertEquals(isServiceRole("Bearer sb_publishable_x"), false);
  assertEquals(isServiceRole(null), false);
});
