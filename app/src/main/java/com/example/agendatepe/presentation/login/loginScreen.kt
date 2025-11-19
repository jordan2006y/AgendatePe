package com.example.agendatepe.presentation.login

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendatepe.R
import com.example.agendatepe.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
// 1. AGREGAMOS EL PARÁMETRO DE NAVEGACIÓN
fun LoginScreen(auth: FirebaseAuth, navigateToHome: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Azul)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ... (Resto del diseño igual: Icono, Textos, TextFields) ...
        Spacer(modifier = Modifier.height(30.dp))
        Row {
            Icon(
                painterResource(id = R.drawable.back),
                contentDescription = "",
                tint = White,
                modifier = Modifier.padding(vertical = 24.dp).size(24.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Text(text = "Email", color = White, fontWeight = FontWeight.Bold, fontSize = 40.sp)
        TextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = UnselectedField, focusedContainerColor = SelectedField
            )
        )
        Spacer(Modifier.height(48.dp))
        Text(text = "Password", color = White, fontWeight = FontWeight.Bold, fontSize = 40.sp)
        TextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = UnselectedField, focusedContainerColor = SelectedField
            )
        )
        Spacer(Modifier.height(48.dp))

        Button(
            onClick = {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i("aris", "Login Ok")
                        // 2. ¡NAVEGAMOS AL HOME!
                        navigateToHome()
                    } else {
                        Log.i("aris", "Login Fail")
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Crema)
        ) {
            Text(text = "Login", color = black)
        }
    }
}