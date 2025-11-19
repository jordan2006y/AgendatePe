package com.example.agendatepe

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

            // 1. LÓGICA DE SESIÓN AUTOMÁTICA
            // ¿Hay usuario? -> "home". ¿No hay? -> "initialScreen"
            val startDestination = if (auth.currentUser != null) "home" else "initialScreen"

            AgendatePeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 2. PASAMOS EL DESTINO CALCULADO
                    NavigationWrapper(navHostController, auth, startDestination)
                }
            }
        }
    }
}