package com.desarrolloMovielexample.huella.e2e

import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Edge Function moderate-ai desplegada en el hosted (docs/MODERACION_IA.md): autenticación, rol y modo sin key. */
class ModerateAiE2eTest {
    companion object {
        @AfterClass @JvmStatic
        fun cleanup() = E2eEnv.cleanup()
    }

    @Before
    fun setUp() = E2eEnv.assumeConfigured()

    private fun call(body: String, token: String?) = E2eEnv.rest("functions/v1/moderate-ai", "POST", body, token)

    private fun publicationOf(author: E2eEnv.TestUser): String {
        val r = E2eEnv.rest(
            "rest/v1/publications", "POST",
            """{"author_id":"${author.id}","title":"E2E moderate-ai","species":"Perro","breed":"Criollo","size":"Mediano","city":"Medellín","description":"Prueba e2e de moderate-ai"}""",
            author.token, headers = mapOf("Prefer" to "return=representation"),
        )
        assertTrue("crear publicación ${r.code}: ${r.body}", r.ok)
        return r.rows().single().get("id").asString
    }

    @Test
    fun `sin JWT responde 401`() {
        val r = call("""{"publication_id":"00000000-0000-0000-0000-000000000000"}""", token = null)
        assertEquals(r.body, 401, r.code)
    }

    @Test
    fun `usuario no moderador recibe 403`() {
        val user = E2eEnv.createUser(name = "No Mod E2E")
        val r = call("""{"publication_id":"${publicationOf(user)}"}""", user.token)
        assertEquals(r.body, 403, r.code)
    }

    @Test
    fun `moderador sin DEEPSEEK_API_KEY recibe not_configured`() {
        val author = E2eEnv.createUser(name = "Autor IA E2E")
        val mod = E2eEnv.createUser(name = "Mod IA E2E")
        E2eEnv.makeModerator(mod)
        val r = call("""{"publication_id":"${publicationOf(author)}"}""", mod.token)
        assertEquals(r.body, 200, r.code)
        // ponytail: si el equipo ya configuró la key, "ok" también es válido (y genera una revisión real).
        assertTrue(r.body, r.json().get("status").asString in setOf("not_configured", "ok"))
    }
}
