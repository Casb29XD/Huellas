package com.desarrolloMovielexample.huella

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
import com.desarrolloMovielexample.huella.data.remote.SupabaseSession
import com.desarrolloMovielexample.huella.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SupabaseSession.initialize(applicationContext)
        SupabaseRepository.initialize(applicationContext)
        setContent {
            HuellaTheme { AppNavigation() }
        }
    }
}
