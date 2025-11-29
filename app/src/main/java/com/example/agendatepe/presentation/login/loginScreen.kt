package com.example.agendatepe.presentation.login

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Dialogo Salida
    var showExitDialog by remember { mutableStateOf(false) }
    BackHandler { showExitDialog = true }
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Cancelar?", color = MainWhite) },
            text = { Text("Se perderán los datos ingresados.", color = TextGray) },
            confirmButton = { Button(onClick = { showExitDialog = false; onBack() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Salir", color = Color.White) } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Continuar", color = MainWhite) } },
            containerColor = DarkSurface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground) // Fondo negro estilo Instagram
            .padding(24.dp)
    ) {
        // Header
        IconButton(onClick = { showExitDialog = true }, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = MainWhite)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Bienvenido de nuevo",
            color = MainWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp
        )
        Text(
            text = "Ingresa tus credenciales para continuar.",
            color = TextGray,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Inputs Sofisticados
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Email, null, tint = TextGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MainBlue,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = MainBlue,
                unfocusedLabelColor = TextGray,
                focusedTextColor = MainWhite,
                unfocusedTextColor = MainWhite,
                cursorColor = MainBlue
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextGray) },
            trailingIcon = {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(image, null, tint = TextGray)
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MainBlue,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = MainBlue,
                unfocusedLabelColor = TextGray,
                focusedTextColor = MainWhite,
                unfocusedTextColor = MainWhite,
                cursorColor = MainBlue
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "¿Olvidaste tu contraseña?",
            color = MainBlue,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.End).clickable { /* TODO: Implementar Reset Password */ }
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) navigateToHome()
                        else Toast.makeText(context, "Error: Verifica tus datos", Toast.LENGTH_SHORT).show()
                    }
                } else { Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show() }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MainBlue)
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("Ingresar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}