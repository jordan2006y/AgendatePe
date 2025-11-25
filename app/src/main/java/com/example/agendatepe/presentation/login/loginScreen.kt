package com.example.agendatepe.presentation.login

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendatepe.R
import com.example.agendatepe.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    auth: FirebaseAuth,
    navigateToHome: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // --- PROTECCIÓN DE SALIDA ---
    var showExitDialog by remember { mutableStateOf(false) }
    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Cancelar inicio de sesión?") },
            text = { Text("Se perderán los datos ingresados.") },
            confirmButton = { Button(onClick = { showExitDialog = false; onBack() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Salir", color = Color.White) } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Continuar", color = black) } },
            containerColor = Color.White
        )
    }
    // ----------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Azul)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Row {
            IconButton(
                onClick = { showExitDialog = true }, // Activamos dialogo aquí
                modifier = Modifier.padding(vertical = 24.dp).size(24.dp)
            ) {
                Icon(
                    painterResource(id = R.drawable.back),
                    contentDescription = "",
                    tint = White
                )
            }
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
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.i("aris", "Login Ok")
                            navigateToHome()
                        } else {
                            Log.i("aris", "Login Fail")
                            Toast.makeText(context, "Error: Cuenta no existe o datos incorrectos", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Llene todos los campos", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Crema)
        ) {
            Text(text = "Login", color = black)
        }
    }
}