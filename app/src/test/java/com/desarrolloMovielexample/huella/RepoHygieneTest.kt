package com.desarrolloMovielexample.huella

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guardas sobre archivos del repo para hallazgos que no son comportamiento en tiempo de ejecución. */
class RepoHygieneTest {
    // Gradle corre las pruebas unitarias desde app/; la raíz es la carpeta con settings.gradle.kts.
    private val root = generateSequence(File("").absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() }

    private fun file(path: String) = File(root, path).readText(Charsets.UTF_8)
    private fun tree(path: String, vararg ext: String) =
        File(root, path).walkTopDown().filter { it.isFile && it.extension in ext }.toList()

    @Test
    fun `item 5 - ninguna migracion trae usuarios ni contrasenas de auth`() {
        val offenders = tree("supabase/migrations", "sql").filter {
            val sql = it.readText().lowercase()
            "insert into auth.users" in sql || "admin1234" in sql || "crypt(" in sql
        }
        assertEquals("migraciones con usuarios/contraseñas: ${offenders.map { it.name }}", emptyList<File>(), offenders)
    }

    @Test
    fun `item 16 - cada perfil del seed usa ON CONFLICT DO UPDATE`() {
        val seed = file("supabase/seed.sql")
        val inserts = Regex("INSERT INTO public\\.profiles", RegexOption.IGNORE_CASE).findAll(seed).count()
        val upserts = Regex("\\) ON CONFLICT \\(id\\) DO UPDATE", RegexOption.IGNORE_CASE).findAll(seed).count()
        assertTrue("el seed no inserta perfiles", inserts > 0)
        assertEquals("perfiles del seed que el trigger dejaría a medias", inserts, upserts)
    }

    @Test
    fun `item 30 - sin mojibake en codigo ni recursos`() {
        // U+FFFD, U+251C y UTF-8 leído como cp1252 (C3 A1 por "á", C2 BF por "¿", etc.).
        val bad = listOf("\uFFFD", "\u251C", "\u00C3\u00A1", "\u00C3\u00A9", "\u00C3\u00AD", "\u00C3\u00B3", "\u00C3\u00BA", "\u00C3\u00B1", "\u00C2\u00BF", "\u00C2\u00A1")
        val offenders = tree("app/src/main", "kt", "xml").filter { f -> f.readText(Charsets.UTF_8).let { t -> bad.any { it in t } } }
        assertEquals("archivos con texto corrupto", emptyList<String>(), offenders.map { it.relativeTo(root).path })
    }

    @Test
    fun `item 33 - sin ciudad fija en la app`() {
        val offenders = tree("app/src/main", "kt", "xml").filter { "cerca de Medellín" in it.readText(Charsets.UTF_8) }
        assertEquals(emptyList<String>(), offenders.map { it.name })
    }

    @Test
    fun `item 35 - la sesion no viaja en backups`() {
        assertTrue(file("app/src/main/AndroidManifest.xml").contains("android:allowBackup=\"false\""))
        val exclude = "<exclude domain=\"sharedpref\" path=\"supabase_session.xml\" />"
        assertTrue("backup_rules.xml", exclude in file("app/src/main/res/xml/backup_rules.xml"))
        val rules = file("app/src/main/res/xml/data_extraction_rules.xml")
        assertEquals("cloud-backup y device-transfer", 2, Regex(Regex.escape(exclude)).findAll(rules).count())
    }

    @Test
    fun `item 38 - el build no fija un JDK local`() {
        assertFalse(file("gradle.properties").lines().any { it.trim().startsWith("org.gradle.java.home") })
    }

    @Test
    fun `item 40 - el logging HTTP solo se agrega en debug`() {
        val client = file("app/src/main/java/com/desarrolloMovielexample/huella/data/remote/RetrofitClient.kt")
        val adds = Regex("addInterceptor\\(loggingInterceptor\\)").findAll(client).toList()
        assertEquals(1, adds.size)
        assertTrue("addInterceptor(loggingInterceptor) sin BuildConfig.DEBUG",
            client.lineSequence().first { "addInterceptor(loggingInterceptor)" in it }.contains("if (BuildConfig.DEBUG)"))
    }

    @Test
    fun `item 33 - sin permisos de ubicacion ni slider de distancia mientras no haya geolocalizacion`() {
        val manifest = file("app/src/main/AndroidManifest.xml")
        assertFalse("permiso de ubicación sin uso", "LOCATION" in manifest)
        assertFalse("filtro de distancia decorativo", "distanceKm" in file("app/src/main/java/com/desarrolloMovielexample/huella/features/feed/HomeViewModel.kt"))
    }

    @Test
    fun `item 56 - sin esperas fijas para que se vea un snackbar`() {
        val offenders = tree("app/src/main/java", "kt").filter { f ->
            f.name != "SplashScreen.kt" && Regex("""delay\(\d""").containsMatchIn(f.readText(Charsets.UTF_8))
        }
        assertEquals(emptyList<String>(), offenders.map { it.name })
    }

    @Test
    fun `item 49 - el onboarding no usa ilustraciones ajenas al copy`() {
        val onboarding = file("app/src/main/java/com/desarrolloMovielexample/huella/features/auth/OnboardingScreen.kt")
        listOf("illus_location", "illus_cool_guy").forEach { assertFalse(it, it in onboarding) }
    }

    @Test
    fun `item 60 - una sola marca y version del build en el pie`() {
        for (path in listOf("app/src/main/res/values/strings.xml", "app/src/main/res/values-en/strings.xml")) {
            val strings = file(path)
            assertTrue(path, "<string name=\"app_name\">Huella</string>" in strings)
            assertFalse("versión fija en $path", "Huella 1.0" in strings)
        }
    }
}
