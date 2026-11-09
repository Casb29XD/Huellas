package com.desarrolloMovielexample.huella

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.core.utils.AppLanguage
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
import com.desarrolloMovielexample.huella.data.remote.SupabaseSession
import com.desarrolloMovielexample.huella.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLanguage.wrapBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SupabaseSession.initialize(applicationContext)
        SupabaseRepository.initialize(applicationContext)

        // Leer el deep link entrante (correo de confirmación o reset de contraseña).
        // Solo PKCE: huella://auth/confirm?code=XXX[&type=recovery] (token_hash/access_token se rechazan, ítem 36).
        val deepLinkUri = intent?.data

        setContent {
            HuellaTheme { AppNavigation(deepLinkUri = deepLinkUri) }
        }
    }

    /** El sistema puede re-entregar el Intent si la app ya está abierta. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Reiniciar con el nuevo deep link relanzando la actividad limpiamente.
        setIntent(intent)
        recreate()
    }
}
