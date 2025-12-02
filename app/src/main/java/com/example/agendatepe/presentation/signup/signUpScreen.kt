package com.example.agendatepe.presentation.signup

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@Composable
fun SingUpScreen(
    navigateToProfile: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // --- PROTECCIÓN DE SALIDA ---
    var showExitDialog by remember { mutableStateOf(false) }

    // Intercepta el botón físico "Atrás"
    BackHandler {
        if (email.isNotEmpty() || password.isNotEmpty()) showExitDialog = true
        else onBack()
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Cancelar registro?", color = MainWhite) },
            text = { Text("Se perderán los datos ingresados.", color = TextGray) },
            confirmButton = {
                Button(
                    onClick = { showExitDialog = false; onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Salir", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continuar", color = MainWhite)
                }
            },
            containerColor = DarkSurface
        )
    }
    // ------------------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        // Icono de Atrás con validación
        IconButton(
            onClick = {
                if (email.isNotEmpty() || password.isNotEmpty()) showExitDialog = true
                else onBack()
            },
            modifier = Modifier.offset(x = (-12).dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = MainWhite)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Crear Cuenta", color = MainWhite, fontWeight = FontWeight.Bold, fontSize = 32.sp)
        Text("Únete a la mejor comunidad inmobiliaria.", color = TextGray, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(48.dp))

        // Email
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Email, null, tint = TextGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MainBlue, unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = MainBlue, unfocusedLabelColor = TextGray,
                focusedTextColor = MainWhite, unfocusedTextColor = MainWhite, cursorColor = MainBlue,
                focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Contraseña
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextGray) },
            trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = TextGray) } },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MainBlue, unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = MainBlue, unfocusedLabelColor = TextGray,
                focusedTextColor = MainWhite, unfocusedTextColor = MainWhite, cursorColor = MainBlue,
                focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Confirmar Contraseña
        OutlinedTextField(
            value = passwordConfirm, onValueChange = { passwordConfirm = it },
            label = { Text("Confirmar Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextGray) },
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if(password == passwordConfirm && password.isNotEmpty()) MainBlue else if (passwordConfirm.isNotEmpty()) Color.Red else Color.DarkGray,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = MainBlue, unfocusedLabelColor = TextGray,
                focusedTextColor = MainWhite, unfocusedTextColor = MainWhite, cursorColor = MainBlue,
                focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    if (password == passwordConfirm) {
                        if (password.length >= 6) navigateToProfile(email, password)
                        else Toast.makeText(context, "Mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
                    } else { Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show() }
                } else { Toast.makeText(context, "Completa todos los datos", Toast.LENGTH_SHORT).show() }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MainBlue)
        ) {
            Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}