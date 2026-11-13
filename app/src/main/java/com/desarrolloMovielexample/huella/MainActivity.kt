package com.desarrolloMovielexample.huella

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.core.utils.AppLanguage
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
import com.desarrolloMovielexample.huella.data.remote.SupabaseSession
import com.desarrolloMovielexample.huella.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    private var deepLinkUri by mutableStateOf<Uri?>(null)

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLanguage.wrapBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SupabaseSession.initialize(applicationContext)
        SupabaseRepository.initialize(applicationContext)

        // Leer el deep link entrante (correo de confirmación o reset de contraseña).
        // Solo PKCE: huella://auth/confirm?code=XXX[&type=recovery].
        deepLinkUri = intent?.data

        setContent {
            HuellaTheme { AppNavigation(deepLinkUri = deepLinkUri) }
        }
    }

    /** Actualiza el callback sin recrear la pantalla ni perder el estado de navegación. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkUri = intent.data
    }
}
