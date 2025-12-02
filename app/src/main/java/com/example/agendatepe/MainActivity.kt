package com.example.agendatepe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.agendatepe.ui.theme.AgendatePeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private lateinit var navHostController: NavHostController
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        enableEdgeToEdge()

        setContent {
            navHostController = rememberNavController()

            // 1. ESTADO GLOBAL DEL TEMA (Por defecto: Oscuro = true)
            var isDarkTheme by remember { mutableStateOf(true) }

            val startDestination = if (auth.currentUser != null) "home" else "initialScreen"

            // 2. APLICAMOS EL TEMA AQUI PARA TODA LA APP
            AgendatePeTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 3. PASAMOS EL ESTADO Y LA FUNCIÓN PARA CAMBIARLO
                    NavigationWrapper(
                        navHostController = navHostController,
                        auth = auth,
                        startDestination = startDestination,
                        isDarkTheme = isDarkTheme,
                        onThemeChange = { isDarkTheme = !isDarkTheme }
                    )
                }
            }
        }
    }
}