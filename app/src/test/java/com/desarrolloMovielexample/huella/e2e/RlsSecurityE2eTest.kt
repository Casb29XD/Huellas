package com.desarrolloMovielexample.huella.e2e

import org.junit.AfterClass
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Exploits reproducidos en la auditoría (ítems 1-4 y 18). Cada prueba ASSERTA que el ataque está bloqueado.
 * Hoy fallan (rojo) a propósito: las migraciones de roles/RLS/storage las ponen en verde. No marcar @Ignore.
 * Cada prueba usa usuarios propios (E2eEnv.createUser) que se borran en @AfterClass.
 */
class RlsSecurityE2eTest {
    companion object {
        @AfterClass @JvmStatic
        fun cleanup() = E2eEnv.cleanup()
    }

    @Before
    fun setUp() = E2eEnv.assumeConfigured()

    private fun profile(id: String, select: String) =
        E2eEnv.admin("rest/v1/profiles?id=eq.$id&select=$select").rows().single()

    /** Ítem 4: con la publishable key, GET profiles?select=email,phone no debe devolver datos personales. */
    @Test
    fun `a - anon no puede leer email ni phone de profiles (rojo hasta B1)`() {
        val victim = E2eEnv.createUser(name = "Victima E2E")
        E2eEnv.admin("rest/v1/profiles?id=eq.${victim.id}", "PATCH", """{"phone":"3000000000"}""")
        val r = E2eEnv.rest("rest/v1/profiles?id=eq.${victim.id}&select=email,phone")
        if (!r.ok) return
        val leaked = r.rows().filter { row ->
            listOf("email", "phone").any { c -> row.get(c)?.takeIf { !it.isJsonNull }?.asString?.isNotBlank() == true }
        }
        assertTrue("anon leyó email/phone de ${victim.email}: ${r.body.take(200)}", leaked.isEmpty())
    }

    /** Ítem 2: PATCH profiles?id=eq.<yo> con points/is_moderator no debe cambiar esas columnas. */
    @Test
    fun `b - usuario no puede subirse points ni is_moderator por PATCH (rojo hasta B1)`() {
        val laura = E2eEnv.createUser(name = "Laura E2E")
        E2eEnv.rest("rest/v1/profiles?id=eq.${laura.id}", "PATCH", """{"points":99999,"is_moderator":true}""", laura.token)
        val after = profile(laura.id, "points,is_moderator")
        assertNotEquals("points cambió a 99999", 99999, after.get("points").asInt)
        assertFalse("laura se volvió moderadora", after.get("is_moderator").asBoolean)
    }

    /** Ítem 1: signup público con data.is_moderator=true no debe crear un moderador. */
    @Test
    fun `c - signup con is_moderator true no crea moderador (rojo hasta B1)`() {
        val email = E2eEnv.uniqueEmail()
        val body = """{"email":"$email","password":"${E2eEnv.PASSWORD}","data":{"name":"E2E","is_moderator":true}}"""
        var r = E2eEnv.rest("auth/v1/signup", "POST", body)
        // ponytail: el SMTP del hosted limita correos de confirmación por hora; con 429 la Admin API crea el usuario con
        // los mismos metadatos, que llegan igual a raw_user_meta_data y a handle_new_user (el punto vulnerable).
        if (r.code == 429) {
            r = E2eEnv.admin(
                "auth/v1/admin/users", "POST",
                """{"email":"$email","password":"${E2eEnv.PASSWORD}","user_metadata":{"name":"E2E","is_moderator":true}}""",
            )
        }
        assertTrue("signup ${r.code}: ${r.body}", r.ok)
        val json = r.json()
        val id = E2eEnv.track((json.getAsJsonObject("user") ?: json).get("id").asString)
        assertFalse("el signup creó un moderador", profile(id, "is_moderator").get("is_moderator").asBoolean)
    }

    /** Ítem 3: el autor no debe poder poner status=APROBADA a su propia publicación. */
    @Test
    fun `d - autor no puede autoaprobar su publicacion por PATCH (rojo hasta B1)`() {
        val laura = E2eEnv.createUser(name = "Laura E2E")
        val created = E2eEnv.rest(
            "rest/v1/publications", "POST",
            """{"author_id":"${laura.id}","title":"E2E autoaprobación","species":"Perro","breed":"Criollo","size":"Mediano","city":"Medellín","description":"Prueba e2e"}""",
            laura.token, headers = mapOf("Prefer" to "return=representation"),
        )
        assertTrue("crear publicación ${created.code}: ${created.body}", created.ok)
        val pubId = created.rows().single().get("id").asString
        E2eEnv.rest("rest/v1/publications?id=eq.$pubId", "PATCH", """{"status":"APROBADA"}""", laura.token)
        val status = E2eEnv.admin("rest/v1/publications?id=eq.$pubId&select=status").rows().single().get("status").asString
        assertNotEquals("el autor autoaprobó su publicación", "APROBADA", status)
    }

    /** Ítem 18: subir a pet-images fuera de la carpeta <uid>/ debe fallar. */
    @Test
    fun `e - storage pet-images rechaza subir fuera de la carpeta del usuario (rojo hasta B1)`() {
        val laura = E2eEnv.createUser(name = "Laura E2E")
        val carlos = E2eEnv.createUser(name = "Carlos E2E")
        val own = "pet-images/${laura.id}/e2e-${UUID.randomUUID()}.jpg"
        val foreign = "pet-images/${carlos.id}/e2e-${UUID.randomUUID()}.jpg"
        try {
            // Control: en su propia carpeta sí debe poder subir; si no, storage está roto y la prueba no demuestra nada.
            val r1 = E2eEnv.rest("storage/v1/object/$own", "POST", "jpg", laura.token, contentType = "image/jpeg")
            assertTrue("storage no acepta ni la carpeta propia: ${r1.code} ${r1.body}", r1.ok)
            val r2 = E2eEnv.rest("storage/v1/object/$foreign", "POST", "jpg", laura.token, contentType = "image/jpeg")
            assertFalse("laura subió a la carpeta de carlos ($foreign): ${r2.code} ${r2.body}", r2.ok)
        } finally {
            E2eEnv.admin("storage/v1/object/pet-images", "DELETE",
                """{"prefixes":["${own.removePrefix("pet-images/")}","${foreign.removePrefix("pet-images/")}"]}""")
        }
    }
}
