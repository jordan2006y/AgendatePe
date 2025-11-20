package com.example.agendatepe.presentation.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendatepe.R
import com.example.agendatepe.ui.theme.* // Importa tus colores (Azul, Crema, black, White)

@Composable
// 1. Recibe una función que acepta (email, password)
fun SingUpScreen(
    navigateToProfile: (String, String) -> Unit,
    onBack: () -> Unit // <-- AÑADIDO: Función para volver atrás
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Azul) // Tu Azul
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Row {
            // CORREGIDO: Usar IconButton y onBack
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(vertical = 24.dp).size(24.dp)
            ) {
                Icon(
                    painterResource(id = R.drawable.back),
                    contentDescription = "Atrás",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Text(text = "Email", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 40.sp)
        TextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = UnselectedField,
                focusedContainerColor = SelectedField
            )
        )

        Spacer(Modifier.height(48.dp))

        Text(text = "Password", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 40.sp)
        TextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = UnselectedField,
                focusedContainerColor = SelectedField
            )
        )

        Spacer(Modifier.height(48.dp))

        // --- BOTÓN CREMA CON LETRAS NEGRAS ---
        Button(
            onClick = {
                // Validamos que no estén vacíos antes de pasar
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    navigateToProfile(email, password) // ¡Pasamos los datos!
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Crema) // FONDO CREMA
        ) {
            Text(text = "Continuar", color = black, fontWeight = FontWeight.Bold) // LETRAS NEGRAS
        }
    }
}