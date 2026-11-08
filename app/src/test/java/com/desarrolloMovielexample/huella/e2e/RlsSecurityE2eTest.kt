package com.desarrolloMovielexample.huella.e2e

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Los 5 exploits reproducidos en la auditoría (ítems 1-4 y 18). Cada prueba ASSERTA que el ataque está bloqueado.
 * Hoy fallan (rojo) a propósito: la fase B1 (migraciones de roles/RLS/storage) las pone en verde. No marcar @Ignore.
 */
class RlsSecurityE2eTest {
    @Before
    fun setUp() = E2eEnv.assumeConfigured()

    /** Ítem 4: con la publishable key, GET profiles?select=email,phone no debe devolver datos personales. */
    @Test
    fun `a - anon no puede leer email ni phone de profiles (rojo hasta B1)`() {
        val r = E2eEnv.rest("rest/v1/profiles?select=email,phone")
        if (!r.ok) return
        val leaked = r.rows().filter { row ->
            listOf("email", "phone").any { c -> row.get(c)?.takeIf { !it.isJsonNull }?.asString?.isNotBlank() == true }
        }
        assertTrue("anon leyó ${leaked.size} perfiles con email/phone: ${r.body.take(200)}", leaked.isEmpty())
    }

    /** Ítem 2: PATCH profiles?id=eq.<yo> con points/is_moderator no debe cambiar esas columnas. */
    @Test
    fun `b - usuario no puede subirse points ni is_moderator por PATCH (rojo hasta B1)`() {
        val (token, id) = E2eEnv.login(E2eEnv.LAURA.first, E2eEnv.LAURA.second)
        val before = E2eEnv.adminQuery("rest/v1/profiles?id=eq.$id&select=points,is_moderator").rows().single()
        try {
            E2eEnv.rest("rest/v1/profiles?id=eq.$id", "PATCH", """{"points":99999,"is_moderator":true}""", token)
            val after = E2eEnv.adminQuery("rest/v1/profiles?id=eq.$id&select=points,is_moderator").rows().single()
            assertNotEquals("points cambió a 99999", 99999, after.get("points").asInt)
            assertFalse("laura se volvió moderadora", after.get("is_moderator").asBoolean)
        } finally {
            E2eEnv.adminQuery("rest/v1/profiles?id=eq.$id", "PATCH", before.toString())
        }
    }

    /** Ítem 1: signup con data.is_moderator=true no debe crear un moderador. */
    @Test
    fun `c - signup con is_moderator true no crea moderador (rojo hasta B1)`() {
        val email = "e2e-${UUID.randomUUID()}@ejemplo.com"
        val r = E2eEnv.rest(
            "auth/v1/signup", "POST",
            """{"email":"$email","password":"E2e-Test-1234!","data":{"name":"E2E","is_moderator":true}}""",
        )
        assertTrue("signup ${r.code}: ${r.body}", r.ok)
        val json = r.json()
        val id = (json.getAsJsonObject("user") ?: json).get("id").asString
        val profile = E2eEnv.adminQuery("rest/v1/profiles?id=eq.$id&select=is_moderator").rows().single()
        assertFalse("el signup creó un moderador", profile.get("is_moderator").asBoolean)
    }

    /** Ítem 3: el autor no debe poder poner status=APROBADA a su propia publicación. */
    @Test
    fun `d - autor no puede autoaprobar su publicacion por PATCH (rojo hasta B1)`() {
        val (token, id) = E2eEnv.login(E2eEnv.LAURA.first, E2eEnv.LAURA.second)
        val created = E2eEnv.rest(
            "rest/v1/publications", "POST",
            """{"author_id":"$id","title":"E2E autoaprobación","species":"Perro","breed":"Criollo","size":"Mediano","city":"Medellín","description":"Prueba e2e"}""",
            token, headers = mapOf("Prefer" to "return=representation"),
        )
        assertTrue("crear publicación ${created.code}: ${created.body}", created.ok)
        val pubId = created.rows().single().get("id").asString
        try {
            E2eEnv.rest("rest/v1/publications?id=eq.$pubId", "PATCH", """{"status":"APROBADA"}""", token)
            val status = E2eEnv.adminQuery("rest/v1/publications?id=eq.$pubId&select=status").rows().single().get("status").asString
            assertNotEquals("el autor autoaprobó su publicación", "APROBADA", status)
        } finally {
            E2eEnv.adminQuery("rest/v1/publications?id=eq.$pubId", "DELETE")
        }
    }

    /** Ítem 18: subir a pet-images fuera de la carpeta <uid>/ debe fallar. */
    @Test
    fun `e - storage pet-images rechaza subir fuera de la carpeta del usuario (rojo hasta B1)`() {
        val (token, lauraId) = E2eEnv.login(E2eEnv.LAURA.first, E2eEnv.LAURA.second)
        // Control: en su propia carpeta sí debe poder subir; si no, storage está roto y la prueba no demuestra nada.
        val own = E2eEnv.rest("storage/v1/object/pet-images/$lauraId/e2e-${UUID.randomUUID()}.png", "POST", "png", token, contentType = "image/png")
        assertTrue("storage no acepta ni la carpeta propia: ${own.code} ${own.body}", own.ok)
        val (_, carlosId) = E2eEnv.login(E2eEnv.CARLOS.first, E2eEnv.CARLOS.second)
        val path = "pet-images/$carlosId/e2e-${UUID.randomUUID()}.png"
        val r = E2eEnv.rest("storage/v1/object/$path", "POST", "png", token, contentType = "image/png")
        assertFalse("laura subió a la carpeta de carlos ($path): ${r.code} ${r.body}", r.ok)
    }
}
